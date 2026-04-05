import Foundation

/// Import/export parity with `ExcelImportExportUtils.kt` — use `ZipArchive` + XML parsing or a Swift xlsx library.
enum ExcelService {
    static func exportItems(_ items: [ItemModel], to url: URL) throws {
        // Minimal CSV fallback for interoperability testing; replace with true xlsx when dependency is added.
        var csv = "uuid,name,description,quantity\n"
        for i in items {
            let name = i.name.replacingOccurrences(of: ",", with: ";")
            let desc = i.itemDescription.replacingOccurrences(of: ",", with: ";")
            csv += "\(i.uuid),\(name),\(desc),\(i.quantity)\n"
        }
        try csv.write(to: url, atomically: true, encoding: .utf8)
    }

    static func parseImport(from url: URL) throws -> [[String]] {
        let s = try String(contentsOf: url, encoding: .utf8)
        return s.split(separator: "\n").map { line in
            line.split(separator: ",").map(String.init)
        }
    }
}
