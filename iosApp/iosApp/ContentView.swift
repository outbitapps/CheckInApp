import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            mapUIViewController: { (pinLat: KotlinFloat, pinLong: KotlinFloat, markerTitle: String) -> UIViewController in
                return UIHostingController(rootView: MapsView(pinLat: pinLat.floatValue, pinLong: pinLong.floatValue, markerTitle: markerTitle))
                        }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



