# LunchVoteApp

A SwiftUI iOS application for coordinating lunch plans with nearby restaurant options. Hosts can invite friends, share a voting link, and automatically select the winning restaurant when the response deadline is reached.

## Features

- Browse a curated list of nearby restaurants with cuisine and distance details.
- Invite friends with a pre-filled share sheet message.
- Track participant responses and allow each person to vote or change their vote until the deadline.
- Live countdown timer that closes voting automatically when time runs out.
- Automatic winner selection with random tie-breaking between the top restaurants.

## Getting Started

1. Open `ios/LunchVoteApp` in Xcode 15 or later.
2. Run the `LunchVoteApp` target on an iOS 17 simulator or device.
3. Use the participant bar to choose the person whose vote you are recording and tap any restaurant row to cast their vote.
4. Once time expires the winning restaurant is displayed to everyone.

## Testing

Unit tests are included in `ios/LunchVoteAppTests/LunchVoteViewModelTests.swift`. Add the folder to an Xcode test target or Swift Package to execute them.
