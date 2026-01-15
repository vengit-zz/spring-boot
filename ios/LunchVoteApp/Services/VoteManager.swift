import Foundation

final class VoteManager {
    private(set) var votes: [UUID: Set<UUID>] = [:]
    private var participantToRestaurant: [UUID: UUID] = [:]
    private let randomIndex: (Int) -> Int

    init(randomIndex: @escaping (Int) -> Int = { count in
        Int.random(in: 0..<count)
    }) {
        self.randomIndex = randomIndex
    }

    func castVote(restaurantID: UUID, participantID: UUID) {
        if let currentRestaurant = participantToRestaurant[participantID], currentRestaurant == restaurantID {
            return
        }

        if let currentRestaurant = participantToRestaurant[participantID] {
            votes[currentRestaurant]?.remove(participantID)
        }

        participantToRestaurant[participantID] = restaurantID
        votes[restaurantID, default: []].insert(participantID)
    }

    func voteCount(for restaurantID: UUID) -> Int {
        votes[restaurantID]?.count ?? 0
    }

    func leadingRestaurants() -> [UUID] {
        guard let maxVotes = votes.values.map(\.count).max(), maxVotes > 0 else {
            return []
        }
        return votes.filter { $0.value.count == maxVotes }.map(\.key)
    }

    func winningRestaurant() -> UUID? {
        let leaders = leadingRestaurants()
        guard !leaders.isEmpty else { return nil }
        if leaders.count == 1 {
            return leaders.first
        }
        let index = randomIndex(leaders.count)
        guard leaders.indices.contains(index) else {
            return leaders.randomElement()
        }
        return leaders[index]
    }
}
