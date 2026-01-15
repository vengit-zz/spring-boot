import Foundation
import Combine

final class LunchVoteViewModel: ObservableObject {
    @Published private(set) var restaurants: [Restaurant]
    @Published private(set) var participants: [Participant]
    @Published private(set) var selectedRestaurant: Restaurant?
    @Published private(set) var timeRemaining: TimeInterval
    @Published private(set) var isVotingClosed: Bool = false

    private let voteManager: VoteManager
    private let currentDateProvider: () -> Date
    private var timerCancellable: AnyCancellable?

    let replyBy: Date

    init(
        restaurants: [Restaurant] = Restaurant.sample,
        participants: [Participant] = Participant.sample,
        replyBy: Date = Calendar.current.date(byAdding: .minute, value: 30, to: .now) ?? .now.addingTimeInterval(1800),
        voteManager: VoteManager = VoteManager(),
        currentDateProvider: @escaping () -> Date = { Date() }
    ) {
        self.restaurants = restaurants
        self.participants = participants
        self.replyBy = replyBy
        self.voteManager = voteManager
        self.currentDateProvider = currentDateProvider
        self.timeRemaining = max(0, replyBy.timeIntervalSince(currentDateProvider()))
        if timeRemaining == 0 {
            closeVoting()
        }
    }

    deinit {
        timerCancellable?.cancel()
    }

    var timeRemainingString: String {
        guard timeRemaining > 0 else { return "00:00" }
        let minutes = Int(timeRemaining) / 60
        let seconds = Int(timeRemaining) % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }

    var invitationMessage: String {
        let formattedDate = replyBy.formatted(date: .omitted, time: .shortened)
        return "Lunch vote is on! Cast your choice before \(formattedDate)."
    }

    func startCountdown() {
        guard timerCancellable == nil, !isVotingClosed else { return }
        timerCancellable = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.tick()
            }
    }

    func voteCount(for restaurantID: UUID) -> Int {
        voteManager.voteCount(for: restaurantID)
    }

    func castVote(for restaurant: Restaurant, by participant: Participant) {
        guard !isVotingClosed else { return }
        voteManager.castVote(restaurantID: restaurant.id, participantID: participant.id)
        objectWillChange.send()
    }

    func isWinning(restaurant: Restaurant) -> Bool {
        if let selectedRestaurant, selectedRestaurant.id == restaurant.id {
            return true
        }
        guard !isVotingClosed else { return false }
        let leaders = voteManager.leadingRestaurants()
        return leaders.contains(restaurant.id)
    }

    func finalizeVoting() {
        closeVoting()
    }

    private func tick() {
        let now = currentDateProvider()
        timeRemaining = max(0, replyBy.timeIntervalSince(now))
        if timeRemaining == 0 {
            closeVoting()
        }
    }

    private func closeVoting() {
        guard !isVotingClosed else { return }
        isVotingClosed = true
        timerCancellable?.cancel()
        if let winnerID = voteManager.winningRestaurant(),
           let restaurant = restaurants.first(where: { $0.id == winnerID }) {
            selectedRestaurant = restaurant
        } else {
            selectedRestaurant = nil
        }
        objectWillChange.send()
    }
}

extension LunchVoteViewModel {
    static var preview: LunchVoteViewModel {
        let replyBy = Calendar.current.date(byAdding: .minute, value: 15, to: .now) ?? .now
        let viewModel = LunchVoteViewModel(replyBy: replyBy)
        viewModel.startCountdown()
        return viewModel
    }
}
