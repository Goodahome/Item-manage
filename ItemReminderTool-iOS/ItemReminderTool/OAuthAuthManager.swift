import AuthenticationServices
import SwiftUI
import UIKit

/// AppAuth parity using `ASWebAuthenticationSession` for OAuth2 code flow.
@MainActor
final class OAuthAuthManager: NSObject, ObservableObject {
    @Published var isPresenting = false

    func startAuthSession(
        authURL: URL,
        callbackScheme: String,
        completion: @escaping (Result<URL, Error>) -> Void
    ) {
        let session = ASWebAuthenticationSession(
            url: authURL,
            callbackURLScheme: callbackScheme
        ) { callback, error in
            if let error {
                completion(.failure(error))
                return
            }
            if let callback {
                completion(.success(callback))
            }
        }
        session.presentationContextProvider = self
        session.start()
    }
}

extension OAuthAuthManager: ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = scene.windows.first else {
            return ASPresentationAnchor()
        }
        return window
    }
}
