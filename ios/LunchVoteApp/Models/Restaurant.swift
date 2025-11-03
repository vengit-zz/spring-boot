import Foundation

struct Restaurant: Identifiable, Hashable {
    let id: UUID
    let name: String
    let cuisine: String
    let address: String
    let distanceMeters: Double

    init(id: UUID = UUID(), name: String, cuisine: String, address: String, distanceMeters: Double) {
        self.id = id
        self.name = name
        self.cuisine = cuisine
        self.address = address
        self.distanceMeters = distanceMeters
    }

    var distanceMiles: Double {
        distanceMeters / 1609.34
    }

    var displayAddress: String {
        "\(address) • \(String(format: "%.1f mi away", distanceMiles))"
    }
}

extension Restaurant {
    static let sample: [Restaurant] = [
        Restaurant(name: "Green Fork", cuisine: "Salads & Bowls", address: "123 Market St", distanceMeters: 320),
        Restaurant(name: "Pasta Fresca", cuisine: "Italian", address: "456 Olive Ave", distanceMeters: 640),
        Restaurant(name: "Taco Loco", cuisine: "Mexican", address: "789 Sunset Blvd", distanceMeters: 950),
        Restaurant(name: "Sushi Mizu", cuisine: "Japanese", address: "101 River Rd", distanceMeters: 1200)
    ]
}
