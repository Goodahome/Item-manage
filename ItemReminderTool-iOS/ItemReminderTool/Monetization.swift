import SwiftUI
import StoreKit
import UIKit

/// StoreKit 2 + AdMob shell — parity with `billing/PremiumFeatureManager` and Play Services Ads.
@MainActor
final class StoreKitManager: ObservableObject {
    @Published var products: [Product] = []
    @Published var purchasedPremium = false

    private let productIds = ["premium_lifetime"]

    func loadProducts() async {
        do {
            products = try await Product.products(for: productIds)
        } catch {
            products = []
        }
    }

    func purchase(_ product: Product) async throws {
        let result = try await product.purchase()
        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            purchasedPremium = true
            UserDefaults.standard.set(true, forKey: "premium_lifetime")
            await transaction.finish()
        case .userCancelled, .pending:
            break
        @unknown default:
            break
        }
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw NSError(domain: "IAP", code: 1)
        case .verified(let safe):
            return safe
        }
    }
}

struct AdMobBannerPlaceholder: UIViewRepresentable {
    /// Replace with `GADBannerView` after adding Google Mobile Ads SDK via SPM.
    func makeUIView(context: Context) -> UIView {
        let v = UIView()
        v.backgroundColor = UIColor.secondarySystemBackground
        return v
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
