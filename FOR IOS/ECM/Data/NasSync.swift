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
    static let server = URL(string: "https://nas.example.com:5006/file/ArxanECM/ecm-data.json")!
    static let username = "nas-admin"
    static let keychainService = "com.ecm.inventory.nas"
    private static let modifiedAtKey = "nasLocalModifiedAt"

    static var localModifiedAt: Double {
        get { UserDefaults.standard.double(forKey: modifiedAtKey) }
        set { UserDefaults.standard.set(newValue, forKey: modifiedAtKey) }
    }

    static var password: String? {
        get {
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: keychainService,
                kSecAttrAccount as String: username,
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
                kSecAttrAccount as String: username
            ]
            SecItemDelete(query as CFDictionary)
            guard let newValue, !newValue.isBlank, let data = newValue.data(using: .utf8) else { return }
            var add = query
            add[kSecValueData as String] = data
            SecItemAdd(add as CFDictionary, nil)
        }
    }
}

private final class NasTrustDelegate: NSObject, URLSessionDelegate {
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        // fnOS 的 WebDAV 使用自动轮换的自签名证书，只对固定 NAS 主机放行。
        if challenge.protectionSpace.host == "nas.example.com",
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

    private func request(method: String, password: String, body: Data? = nil) -> URLRequest {
        var request = URLRequest(url: NasCredentials.server)
        request.httpMethod = method
        request.timeoutInterval = 20
        request.httpBody = body
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let token = Data("\(NasCredentials.username):\(password)".utf8).base64EncodedString()
        request.setValue("Basic \(token)", forHTTPHeaderField: "Authorization")
        return request
    }

    func upload(_ snapshot: EcmSnapshot, password: String) async throws {
        let body = try JSONEncoder().encode(snapshot)
        let (_, response) = try await session.data(for: request(method: "PUT", password: password, body: body))
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw URLError(.cannotWriteToFile)
        }
    }

    func download(password: String) async throws -> EcmSnapshot? {
        let (data, response) = try await session.data(for: request(method: "GET", password: password))
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        if http.statusCode == 404 { return nil }
        guard (200...299).contains(http.statusCode) else { throw URLError(.userAuthenticationRequired) }
        return try JSONDecoder().decode(EcmSnapshot.self, from: data)
    }
}
