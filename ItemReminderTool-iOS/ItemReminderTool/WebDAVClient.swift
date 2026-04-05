import Foundation

/// OkHttp WebDAV parity — PROPFIND / PUT / DELETE via `URLSession`.
actor WebDAVClient {
    private var baseURL: URL
    private let session: URLSession

    init(baseURL: URL, user: String, password: String) {
        self.baseURL = baseURL
        let config = URLSessionConfiguration.default
        if let auth = "\(user):\(password)".data(using: .utf8)?.base64EncodedString() {
            config.httpAdditionalHeaders = ["Authorization": "Basic \(auth)"]
        }
        self.session = URLSession(configuration: config)
    }

    func put(data: Data, path: String) async throws {
        let url = baseURL.appendingPathComponent(path)
        var req = URLRequest(url: url)
        req.httpMethod = "PUT"
        req.httpBody = data
        let (_, res) = try await session.data(for: req)
        guard let http = res as? HTTPURLResponse, (200 ... 299).contains(http.statusCode) else {
            throw NSError(domain: "WebDAV", code: 1)
        }
    }
}
