import SwiftUI

struct ParticipantBadge: View {
    let participant: Participant
    let isSelected: Bool

    var body: some View {
        VStack {
            Text(participant.initials)
                .font(.headline)
                .frame(width: 48, height: 48)
                .background(circleBackground)
                .clipShape(Circle())
                .overlay(alignment: .topTrailing) {
                    if isSelected {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption2)
                            .foregroundStyle(.white, .green)
                            .offset(x: 8, y: -8)
                    }
                }
            Text(participant.firstName)
                .font(.caption)
        }
        .padding(8)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var circleBackground: some ShapeStyle {
        isSelected ? AnyShapeStyle(.blue.gradient) : AnyShapeStyle(.gray.opacity(0.2))
    }
}

#Preview {
    HStack {
        ParticipantBadge(participant: Participant.sample[0], isSelected: false)
        ParticipantBadge(participant: Participant.sample[1], isSelected: true)
    }
}
