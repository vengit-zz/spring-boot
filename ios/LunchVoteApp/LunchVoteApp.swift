import SwiftUI

@main
struct LunchVoteApp: App {
    @StateObject private var viewModel = LunchVoteViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: viewModel)
        }
    }
}
