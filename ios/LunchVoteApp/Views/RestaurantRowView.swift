import SwiftUI

struct RestaurantRowView: View {
    let restaurant: Restaurant
    let votes: Int
    let isSelected: Bool
    let onVote: () -> Void

    var body: some View {
        Button(action: onVote) {
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(restaurant.name)
                        .font(.headline)
                    Text(restaurant.cuisine)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(restaurant.displayAddress)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Label("\(votes)", systemImage: "person.3.fill")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(String(format: "%.1f mi", restaurant.distanceMiles))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(background)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Color.green : .clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }

    private var background: some View {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
            .fill(Color(.systemBackground).opacity(0.9))
            .shadow(color: .black.opacity(0.05), radius: 10, x: 0, y: 5)
    }
}

#Preview {
    RestaurantRowView(
        restaurant: Restaurant.sample[0],
        votes: 4,
        isSelected: true,
        onVote: {}
    )
}
