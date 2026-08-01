import Foundation
import Security

struct EcmSnapshot: Codable {
    var schemaVersion: Int = 1
    var modifiedAt: Double
    var components: [ComponentEntity]
    var locations: [LocationEntity]
    var consumptions: [ConsumptionEntity]
}

enum NasSyncState: Equatable {
    case notConfigured, syncing, synced(Date), failed(String)
}

struct NasCredentials {
    static let keychainService = "com.ecm.inventory.nas"
    private static let keychainAccount = "nas_password"
    private static let modifiedAtKey = "nasLocalModifiedAt"

    static var serverAddress: String {
        get { UserDefaults.standard.string(forKey: "nasServerAddress") ?? "" }
        set { UserDefaults.standard.set(newValue.trimmed, forKey: "nasServerAddress") }
    }

    static var port: Int {
        get {
            let saved = UserDefaults.standard.integer(forKey: "nasPort")
            return saved == 0 ? 5006 : saved
        }
        set { UserDefaults.standard.set(newValue, forKey: "nasPort") }
    }

    static var username: String {
        get { UserDefaults.standard.string(forKey: "nasUsername") ?? "" }
        set { UserDefaults.standard.set(newValue.trimmed, forKey: "nasUsername") }
    }

    static var localModifiedAt: Double {
        get { UserDefaults.standard.double(forKey: modifiedAtKey) }
        set { UserDefaults.standard.set(newValue, forKey: modifiedAtKey) }
    }

    static var password: String? {
        get {
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: keychainService,
                kSecAttrAccount as String: keychainAccount,
                kSecReturnData as String: true
            ]
            var result: CFTypeRef?
            guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
                  let data = result as? Data else { return nil }
            return String(data: data, encoding: .utf8)
        }
        set {
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: keychainService,
                kSecAttrAccount as String: keychainAccount
            ]
            SecItemDelete(query as CFDictionary)
            guard let newValue, !newValue.isBlank, let data = newValue.data(using: .utf8) else { return }
            var add = query
            add[kSecValueData as String] = data
            SecItemAdd(add as CFDictionary, nil)
        }
    }

    static var configuration: NasConfiguration? {
        guard let password, !username.isBlank, (1...65535).contains(port) else { return nil }
        let raw = serverAddress.trimmingCharacters(in: .whitespacesAndNewlines)
        let candidate = raw.contains("://") ? raw : "https://\(raw)"
        guard var parts = URLComponents(string: candidate), let host = parts.host, !host.isEmpty else { return nil }
        parts.scheme = "https"
        parts.port = port
        parts.path = "/file/ArxanECM/ecm-data.json"
        parts.query = nil
        parts.fragment = nil
        guard let fileURL = parts.url else { return nil }
        parts.path = "/file/ArxanECM/"
        guard let directoryURL = parts.url else { return nil }
        return NasConfiguration(host: host, username: username, password: password, fileURL: fileURL, directoryURL: directoryURL)
    }
}

struct NasConfiguration: Sendable {
    let host: String
    let username: String
    let password: String
    let fileURL: URL
    let directoryURL: URL
}

private final class NasTrustDelegate: NSObject, URLSessionDelegate {
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        // fnOS 可能使用自动轮换的自签名证书，只对用户当前配置的 NAS 主机放行。
        if challenge.protectionSpace.host == NasCredentials.configuration?.host,
           challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
           let trust = challenge.protectionSpace.serverTrust {
            completionHandler(.useCredential, URLCredential(trust: trust))
        } else {
            completionHandler(.performDefaultHandling, nil)
        }
    }
}

actor NasSyncClient {
    private let session = URLSession(configuration: .ephemeral, delegate: NasTrustDelegate(), delegateQueue: nil)

    private func request(method: String, url: URL, configuration: NasConfiguration, body: Data? = nil) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 20
        request.httpBody = body
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let token = Data("\(configuration.username):\(configuration.password)".utf8).base64EncodedString()
        request.setValue("Basic \(token)", forHTTPHeaderField: "Authorization")
        return request
    }

    private func ensureDirectory(_ configuration: NasConfiguration) async throws {
        let (_, response) = try await session.data(for: request(method: "MKCOL", url: configuration.directoryURL, configuration: configuration))
        guard let http = response as? HTTPURLResponse, http.statusCode == 201 || http.statusCode == 405 else {
            throw URLError(.cannotCreateFile)
        }
    }

    func upload(_ snapshot: EcmSnapshot, configuration: NasConfiguration) async throws {
        try await ensureDirectory(configuration)
        let body = try JSONEncoder().encode(snapshot)
        let (_, response) = try await session.data(for: request(method: "PUT", url: configuration.fileURL, configuration: configuration, body: body))
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw URLError(.cannotWriteToFile)
        }
    }

    func download(configuration: NasConfiguration) async throws -> EcmSnapshot? {
        let (data, response) = try await session.data(for: request(method: "GET", url: configuration.fileURL, configuration: configuration))
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        if http.statusCode == 404 { return nil }
        guard (200...299).contains(http.statusCode) else { throw URLError(.userAuthenticationRequired) }
        return try JSONDecoder().decode(EcmSnapshot.self, from: data)
    }
}
