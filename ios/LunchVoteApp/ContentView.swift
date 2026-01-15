import SwiftUI

struct ContentView: View {
    @ObservedObject var viewModel: LunchVoteViewModel
    @State private var selectedParticipant: Participant?

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 24) {
                header
                countdown
                participantSelector
                restaurantList
                Spacer(minLength: 0)
                footer
            }
            .padding()
            .navigationTitle("Lunch Vote")
        }
        .onAppear {
            viewModel.startCountdown()
            if selectedParticipant == nil {
                selectedParticipant = viewModel.participants.first
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Invite your team")
                .font(.largeTitle)
                .fontWeight(.bold)
            Text("Choose a restaurant together. Voting closes at \(viewModel.replyBy, format: .dateTime.hour().minute())")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }

    private var countdown: some View {
        HStack {
            Label("Time remaining", systemImage: "timer")
                .font(.headline)
            Spacer()
            Text(viewModel.timeRemainingString)
                .font(.title2)
                .monospacedDigit()
                .foregroundColor(viewModel.timeRemaining > 0 ? .primary : .red)
        }
        .padding()
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var participantSelector: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Who's voting?")
                .font(.headline)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.participants) { participant in
                        ParticipantBadge(
                            participant: participant,
                            isSelected: participant == selectedParticipant
                        )
                        .onTapGesture {
                            selectedParticipant = participant
                        }
                    }
                }
            }
        }
    }

    private var restaurantList: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Nearby restaurants")
                .font(.headline)
            ForEach(viewModel.restaurants) { restaurant in
                RestaurantRowView(
                    restaurant: restaurant,
                    votes: viewModel.voteCount(for: restaurant.id),
                    isSelected: viewModel.isWinning(restaurant: restaurant)
                ) {
                    if let participant = selectedParticipant {
                        viewModel.castVote(for: restaurant, by: participant)
                    }
                }
                .disabled(viewModel.isVotingClosed)
            }
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let winner = viewModel.selectedRestaurant {
                Label("\(winner.name) is the pick!", systemImage: "checkmark.seal.fill")
                    .font(.title3)
                    .foregroundColor(.green)
                    .transition(.opacity)
            } else if viewModel.isVotingClosed {
                Label("Votes are closed", systemImage: "lock.fill")
                    .foregroundColor(.secondary)
            }

            ShareLink(item: viewModel.invitationMessage) {
                Label("Invite friends", systemImage: "square.and.arrow.up")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

#Preview {
    ContentView(viewModel: LunchVoteViewModel.preview)
}
