//
//  activityLiveActivity.swift
//  activity
//
//  Created by Payton Curry on 11/3/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import ActivityKit
import WidgetKit
import SwiftUI
import ComposeApp


struct activityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic stateful properties about your activity go here!
        var distance: Double
        var makingProgress: Bool
        var batteryLevel: Double
        var lastUpdated: Date
    }

    // Fixed non-changing properties about your activity go here!
    var host: String
    var destinationDistance: Double
    var familyID: String
}

struct activityLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: activityAttributes.self) { context in
            // Lock screen/banner UI goes here
            VStack(alignment: .leading) {
                HStack {
                    Image(systemName: "person")
                    Text(context.attributes.host)
                    Spacer()
                    
                    if (context.state.makingProgress) {
                        HStack {
                            Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                            Text("Making progress")
                        }
                    } else {
                        HStack {
                            Image(systemName: "exclamationmark.circle.fill").foregroundStyle(.orange)
                            Text("No progress")
                        }
                    }
                }
                ProgressView(value: context.state.distance, total: context.attributes.destinationDistance).tint(context.state.makingProgress ? Color.green : Color.orange)
                HStack {
                    Text("\(Image(systemName: "battery.75percent")) \((context.state.batteryLevel) * 100, specifier: "%.0f")%")
                    Spacer()
                    Text("\(Image(systemName: "location.circle.fill")) \((context.state.distance / 1609), specifier: "%.2f")mi")
                    Spacer()
                    Text("\(Image(systemName: "mappin.circle.fill")) \((context.attributes.destinationDistance / 1609), specifier: "%.2f")mi")
                    
                }
                HStack {
                    Spacer()
                    Text("Last updated \(context.state.lastUpdated.formatted(date: .omitted, time: .shortened))").foregroundStyle(.secondary)
                    Spacer()
                }
                
            }.padding()
            .activityBackgroundTint(Color.cyan)
            .activitySystemActionForegroundColor(Color.black)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI goes here.  Compose the expanded UI through
                // various regions, like leading/trailing/center/bottom
                DynamicIslandExpandedRegion(.center) {
                    VStack(alignment: .leading) {
                    HStack {
                        Image(systemName: "person")
                        Text(context.attributes.host)
                        Spacer()
                        
                        if (context.state.makingProgress) {
                            HStack {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                                Text("Making progress")
                            }
                        } else {
                            HStack {
                                Image(systemName: "exclamationmark.circle.fill").foregroundStyle(.orange)
                                Text("No progress")
                            }
                        }
                    }
                    ProgressView(value: context.state.distance, total: context.attributes.destinationDistance).tint(context.state.makingProgress ? Color.green : Color.orange)
                    HStack {
                        Text("\(Image(systemName: "battery.75percent")) \((context.state.batteryLevel) * 100, specifier: "%.0f")%")
                        Spacer()
                        Text("\(Image(systemName: "location.circle.fill")) \((context.state.distance / 1609), specifier: "%.2f")mi")
                        Spacer()
                        Text("\(Image(systemName: "mappin.circle.fill")) \((context.attributes.destinationDistance / 1609), specifier: "%.2f")mi")
                        
                    }
                    HStack {
                        Spacer()
                        Text("Last updated \(context.state.lastUpdated.formatted(date: .omitted, time: .shortened))").foregroundStyle(.secondary)
                        Spacer()
                    }
                    
                }
            }
                
            } compactLeading: {
                if (context.state.makingProgress) {
                    
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                } else {
                    
                        Image(systemName: "exclamationmark.circle.fill").foregroundStyle(.orange)
                }
            } compactTrailing: {
                Text("\((context.state.distance / 1609), specifier: "%.2f")mi")
            } minimal: {
                if (context.state.makingProgress) {
                    
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                } else {
                    
                        Image(systemName: "exclamationmark.circle.fill").foregroundStyle(.orange)
                }
            }
            .widgetURL(URL(string: "checkinapp://open/\(context.attributes.familyID)"))
            .keylineTint(Color.red)
        }
    }
}

extension activityAttributes {
    fileprivate static var preview: activityAttributes {
        activityAttributes(host: "Payton", destinationDistance: 196340, familyID: "")
    }
}

extension activityAttributes.ContentState {
    fileprivate static var makingProgress: activityAttributes.ContentState {
        activityAttributes.ContentState(distance: 144841, makingProgress: true, batteryLevel: 0.92, lastUpdated: Date())
     }
    fileprivate static var losingProgress: activityAttributes.ContentState {
        .init(distance: 15000, makingProgress: false, batteryLevel: 0.32, lastUpdated: Date())
    }
}

#Preview("Notification", as: .content, using: activityAttributes.preview) {
   activityLiveActivity()
} contentStates: {
    activityAttributes.ContentState.makingProgress
    activityAttributes.ContentState.losingProgress
}
