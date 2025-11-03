import Foundation

struct Participant: Identifiable, Hashable {
    let id: UUID
    let firstName: String
    let lastName: String

    init(id: UUID = UUID(), firstName: String, lastName: String) {
        self.id = id
        self.firstName = firstName
        self.lastName = lastName
    }

    var initials: String {
        let first = firstName.first.map(String.init) ?? ""
        let last = lastName.first.map(String.init) ?? ""
        return (first + last).uppercased()
    }
}

extension Participant {
    static let sample: [Participant] = [
        Participant(firstName: "Alex", lastName: "Johnson"),
        Participant(firstName: "Taylor", lastName: "Nguyen"),
        Participant(firstName: "Jordan", lastName: "Kim"),
        Participant(firstName: "Sam", lastName: "Patel"),
        Participant(firstName: "Morgan", lastName: "Lopez")
    ]
}
