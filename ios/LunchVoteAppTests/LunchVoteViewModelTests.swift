import XCTest
@testable import LunchVoteApp

final class LunchVoteViewModelTests: XCTestCase {
    func testCastingVoteUpdatesCounts() {
        let restaurants = [
            Restaurant(name: "Cafe 1", cuisine: "Fusion", address: "1 Main St", distanceMeters: 100),
            Restaurant(name: "Cafe 2", cuisine: "BBQ", address: "2 Main St", distanceMeters: 200)
        ]
        let participants = [Participant(firstName: "Alex", lastName: "Doe")]
        let viewModel = LunchVoteViewModel(
            restaurants: restaurants,
            participants: participants,
            replyBy: Date().addingTimeInterval(600),
            voteManager: VoteManager()
        )

        viewModel.castVote(for: restaurants[0], by: participants[0])
        XCTAssertEqual(viewModel.voteCount(for: restaurants[0].id), 1)
        XCTAssertEqual(viewModel.voteCount(for: restaurants[1].id), 0)

        viewModel.castVote(for: restaurants[1], by: participants[0])
        XCTAssertEqual(viewModel.voteCount(for: restaurants[0].id), 0)
        XCTAssertEqual(viewModel.voteCount(for: restaurants[1].id), 1)
    }

    func testFinalizeVotingSelectsHighestVoteGetter() {
        let restaurants = [
            Restaurant(name: "Cafe 1", cuisine: "Fusion", address: "1 Main St", distanceMeters: 100),
            Restaurant(name: "Cafe 2", cuisine: "BBQ", address: "2 Main St", distanceMeters: 200)
        ]
        let participants = [
            Participant(firstName: "Alex", lastName: "Doe"),
            Participant(firstName: "Jamie", lastName: "Smith"),
            Participant(firstName: "Riley", lastName: "Jones")
        ]
        let viewModel = LunchVoteViewModel(
            restaurants: restaurants,
            participants: participants,
            replyBy: Date().addingTimeInterval(600),
            voteManager: VoteManager()
        )

        viewModel.castVote(for: restaurants[0], by: participants[0])
        viewModel.castVote(for: restaurants[0], by: participants[1])
        viewModel.castVote(for: restaurants[1], by: participants[2])

        viewModel.finalizeVoting()

        XCTAssertTrue(viewModel.isVotingClosed)
        XCTAssertEqual(viewModel.selectedRestaurant, restaurants[0])
    }

    func testFinalizeVotingBreaksTieRandomly() {
        let restaurants = [
            Restaurant(name: "Cafe 1", cuisine: "Fusion", address: "1 Main St", distanceMeters: 100),
            Restaurant(name: "Cafe 2", cuisine: "BBQ", address: "2 Main St", distanceMeters: 200)
        ]
        let participants = [
            Participant(firstName: "Alex", lastName: "Doe"),
            Participant(firstName: "Jamie", lastName: "Smith")
        ]
        let viewModel = LunchVoteViewModel(
            restaurants: restaurants,
            participants: participants,
            replyBy: Date().addingTimeInterval(600),
            voteManager: VoteManager(randomIndex: { _ in 1 })
        )

        viewModel.castVote(for: restaurants[0], by: participants[0])
        viewModel.castVote(for: restaurants[1], by: participants[1])

        viewModel.finalizeVoting()

        XCTAssertEqual(viewModel.selectedRestaurant, restaurants[1])
    }
}
