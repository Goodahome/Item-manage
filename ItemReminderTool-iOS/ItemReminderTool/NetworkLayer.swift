import Foundation

// MARK: - API envelope (ApiResponse.kt)

struct APIResponse<T: Decodable>: Decodable {
    let success: Bool
    let data: T?
    let error: APIErrorBody?
}

struct APIErrorBody: Decodable {
    let code: String
    let message: String
}

// MARK: - List wrappers

struct ItemListResponseDTO: Decodable {
    let items: [ItemDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

struct CategoryListResponseDTO: Decodable {
    let categories: [CategoryDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

struct WarehouseListResponseDTO: Decodable {
    let warehouses: [WarehouseDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

struct ShoppingItemListResponseDTO: Decodable {
    let shoppingItems: [ShoppingItemDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

struct ItemReminderListResponseDTO: Decodable {
    let reminders: [ItemReminderDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

struct DeletedRecordListResponseDTO: Decodable {
    let records: [DeletedRecordDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

struct IconLibraryItemListResponseDTO: Decodable {
    let items: [IconLibraryItemDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

// MARK: - DTOs (field names match Gson SerializedName)

struct ItemDTO: Codable {
    let uuid: String
    let name: String
    let description: String?
    let categoryUuid: String?
    let warehouseUuid: String?
    let tags: [String]?
    let purchaseDate: String?
    let expiryDate: String?
    let price: Double?
    let quantity: Int?
    let quantityUnit: String?
    let barcode: String?
    let imageUri: String?
    let imageUris: [String]?
    let primaryImageIndex: Int?
    let featureCode: String?
    let enableStockAlert: Bool?
    let createdAt: String?
    let updatedAt: String?
}

struct CategoryDTO: Codable {
    let uuid: String
    let name: String
    let description: String?
    let color: String?
    let icon: String?
}

struct WarehouseDTO: Codable {
    let uuid: String
    let name: String
    let description: String?
    let location: String?
    let capacity: Int?
    let parentUuid: String?
    let level: Int?
    let imageUri: String?
    let createdAt: String?
    let itemsSuffix: String?
    let hideUseButton: Bool?
    let hideDetailsButton: Bool?
    let hideQuantity: Bool?
    let hideQuantitySlider: Bool?
}

struct ShoppingItemDTO: Codable {
    let uuid: String
    let name: String
    let description: String?
    let quantity: Int?
    let isCompleted: Bool?
    let priority: String?
    let createdAt: String?
    let completedAt: String?
    let imageUri: String?
    let itemUuid: String?
}

struct ItemReminderDTO: Codable {
    let uuid: String
    let itemUuid: String
    let reminderType: String
    let reminderTime: String?
    let dailyTime: String?
    let monthlyDay: Int?
    let monthlyTime: String?
    let yearlyMonth: Int?
    let yearlyDay: Int?
    let yearlyTime: String?
    let reason: String?
    let isEnabled: Bool?
    let createdAt: String?
    let updatedAt: String?
}

struct DeletedRecordDTO: Codable {
    let uuid: String
    let entityType: String
    let entityUuid: String
    let deletedAt: String?
}

struct IconLibraryItemDTO: Codable {
    let uuid: String
    let name: String
    let imagePath: String?
    let iconKey: String?
    let fileSize: Int64?
    let createdAt: Int64?
    let updatedAt: Int64?
}

struct ActivityEventDTO: Codable {
    let uuid: String
    let type: String
    let title: String
    let description: String?
    let targetUuid: String?
    let targetName: String?
    let iconType: String?
    let createdAt: String?
    let metadata: String?
}

struct AuthResponseDTO: Codable {
    let accessToken: String?
    let refreshToken: String?
    let expiresIn: Int?
}

struct LoginRequestDTO: Codable {
    let email: String
    let password: String
}

struct RegisterRequestDTO: Codable {
    let email: String
    let password: String
}

// MARK: - URLSession client (RetrofitClient parity)

actor APIClient {
    static let shared = APIClient()

    private var baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    init() {
        let urlString = UserDefaults.standard.string(forKey: "server_url") ?? "http://localhost:3000"
        self.baseURL = URL(string: urlString) ?? URL(string: "http://localhost:3000")!
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config)
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
    }

    func updateBaseURL(_ string: String) {
        if let u = URL(string: string) {
            baseURL = u
            UserDefaults.standard.set(string, forKey: "server_url")
        }
    }

    private func makeURL(_ pathAndQuery: String) throws -> URL {
        let root = baseURL.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let path = pathAndQuery.hasPrefix("/") ? String(pathAndQuery.dropFirst()) : pathAndQuery
        guard let url = URL(string: "\(root)/\(path)") else {
            throw NSError(domain: "API", code: 0, userInfo: [NSLocalizedDescriptionKey: "Bad URL"])
        }
        return url
    }

    private func authorizedRequest(path: String, method: String, body: Data?) throws -> URLRequest {
        let url = try makeURL(path)
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token = UserDefaults.standard.string(forKey: "access_token"), !token.isEmpty {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        req.httpBody = body
        return req
    }

    func getItems(page: Int = 1, pageSize: Int = 50) async throws -> ItemListResponseDTO {
        let q = "api/items?page=\(page)&pageSize=\(pageSize)"
        let req = try authorizedRequest(path: q, method: "GET", body: nil)
        let (data, _) = try await session.data(for: req)
        let envelope = try decoder.decode(APIResponse<ItemListResponseDTO>.self, from: data)
        guard envelope.success, let d = envelope.data else {
            throw NSError(domain: "API", code: 1, userInfo: [NSLocalizedDescriptionKey: envelope.error?.message ?? "Request failed"])
        }
        return d
    }

    func upsertItem(_ item: ItemDTO) async throws -> ItemDTO {
        let body = try encoder.encode(item)
        var req = try authorizedRequest(path: "api/items", method: "POST", body: body)
        let (data, _) = try await session.data(for: req)
        let envelope = try decoder.decode(APIResponse<ItemDTO>.self, from: data)
        guard envelope.success, let d = envelope.data else {
            throw NSError(domain: "API", code: 2, userInfo: [NSLocalizedDescriptionKey: envelope.error?.message ?? "upsert item"])
        }
        return d
    }

    func login(email: String, password: String) async throws {
        let dto = LoginRequestDTO(email: email, password: password)
        let body = try encoder.encode(dto)
        let req = try authorizedRequest(path: "api/auth/login", method: "POST", body: body)
        let (data, _) = try await session.data(for: req)
        let envelope = try decoder.decode(APIResponse<AuthResponseDTO>.self, from: data)
        guard envelope.success, let auth = envelope.data else {
            throw NSError(domain: "API", code: 3, userInfo: [NSLocalizedDescriptionKey: envelope.error?.message ?? "login"])
        }
        if let t = auth.accessToken {
            UserDefaults.standard.set(t, forKey: "access_token")
        }
        if let r = auth.refreshToken {
            UserDefaults.standard.set(r, forKey: "refresh_token")
        }
    }
}
