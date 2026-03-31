package org.example.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent D: iOS/Swift Code Generator
 * Generates SwiftUI applications for iOS from architecture specifications
 */
@Service
public class DiOSAgent {

    /**
     * Generate complete iOS SwiftUI project
     */
    public iOSProjectOutput generateiOSProject(iOSProjectRequest request) {
        iOSProjectOutput output = new iOSProjectOutput();
        output.setTimestamp(LocalDateTime.now());
        output.setProjectName(request.getProjectName());
        output.setTargetiOSVersion(request.getTargetiOSVersion());

        // Generate SwiftUI views
        output.setAppView(generateAppView(request));
        output.setContentView(generateContentView(request));
        output.setDetailView(generateDetailView(request));
        output.setSettingsView(generateSettingsView(request));

        // Generate models
        output.setDataModels(generateDataModels(request));

        // Generate view models
        output.setViewModels(generateViewModels(request));

        // Generate networking layer
        output.setNetworkingCode(generateNetworking(request));

        // Generate CoreData if persistence needed
        if (request.isPersistenceEnabled()) {
            output.setPersistenceCode(generateCoreData(request));
        }

        // Generate build configuration
        output.setBuildConfiguration(generateBuildConfig(request));

        // Generate Package.swift
        output.setPackageManifest(generatePackageManifest(request));

        // Generate unit tests
        output.setUnitTests(generateUnitTests(request));

        // Generate UI tests
        output.setUiTests(generateUITests(request));

        output.setGenerationSuccess(true);
        output.setStatusMessage("iOS SwiftUI project generated successfully");
        output.setLinesOfCode(estimateLinesOfCode(output));

        return output;
    }

    /**
     * Generate main App view with scene setup
     */
    private String generateAppView(iOSProjectRequest request) {
        StringBuilder view = new StringBuilder();
        view.append("import SwiftUI\n\n");
        view.append("@main\n");
        view.append("struct ").append(request.getProjectName()).append("App: App {\n");
        view.append("    @StateObject private var viewModel = AppViewModel()\n");
        view.append("    \n");
        view.append("    var body: some Scene {\n");
        view.append("        WindowGroup {\n");
        view.append("            ContentView()\n");
        view.append("                .environmentObject(viewModel)\n");
        view.append("        }\n");
        view.append("    }\n");
        view.append("}\n");
        return view.toString();
    }

    /**
     * Generate main content view
     */
    private String generateContentView(iOSProjectRequest request) {
        StringBuilder view = new StringBuilder();
        view.append("import SwiftUI\n\n");
        view.append("struct ContentView: View {\n");
        view.append("    @EnvironmentObject var viewModel: AppViewModel\n");
        view.append("    @State private var showingSettings = false\n");
        view.append("    \n");
        view.append("    var body: some View {\n");
        view.append("        NavigationStack {\n");
        view.append("            List {\n");
        view.append("                ForEach(viewModel.items) { item in\n");
        view.append("                    NavigationLink(destination: DetailView(item: item)) {\n");
        view.append("                        ItemRow(item: item)\n");
        view.append("                    }\n");
        view.append("                }\n");
        view.append("                .onDelete(perform: viewModel.deleteItems)\n");
        view.append("            }\n");
        view.append("            .navigationTitle(\"").append(request.getProjectName()).append("\")\n");
        view.append("            .toolbar {\n");
        view.append("                ToolbarItem(placement: .navigationBarLeading) {\n");
        view.append("                    EditButton()\n");
        view.append("                }\n");
        view.append("                ToolbarItem(placement: .navigationBarTrailing) {\n");
        view.append("                    Button(action: { showingSettings = true }) {\n");
        view.append("                        Image(systemName: \"gear\")\n");
        view.append("                    }\n");
        view.append("                }\n");
        view.append("            }\n");
        view.append("        }\n");
        view.append("        .sheet(isPresented: $showingSettings) {\n");
        view.append("            SettingsView()\n");
        view.append("        }\n");
        view.append("    }\n");
        view.append("}\n");
        return view.toString();
    }

    /**
     * Generate detail view for item display
     */
    private String generateDetailView(iOSProjectRequest request) {
        StringBuilder view = new StringBuilder();
        view.append("import SwiftUI\n\n");
        view.append("struct DetailView: View {\n");
        view.append("    let item: AppItem\n");
        view.append("    @Environment(\\.dismiss) var dismiss\n");
        view.append("    @State private var editMode = EditMode.inactive\n");
        view.append("    \n");
        view.append("    var body: some View {\n");
        view.append("        Form {\n");
        view.append("            Section(header: Text(\"Details\")) {\n");
        view.append("                Text(\"Name: \\(item.name)\")\n");
        view.append("                Text(\"Created: \\(item.createdDate.formatted(date: .abbreviated, time: .shortened))\")\n");
        view.append("            }\n");
        view.append("            Section(header: Text(\"Status\")) {\n");
        view.append("                HStack {\n");
        view.append("                    Text(\"Active\")\n");
        view.append("                    Spacer()\n");
        view.append("                    Text(item.isActive ? \"Yes\" : \"No\")\n");
        view.append("                        .foregroundColor(item.isActive ? .green : .red)\n");
        view.append("                }\n");
        view.append("            }\n");
        view.append("        }\n");
        view.append("        .navigationBarTitleDisplayMode(.inline)\n");
        view.append("        .toolbar {\n");
        view.append("            ToolbarItem(placement: .navigationBarTrailing) {\n");
        view.append("                EditButton()\n");
        view.append("            }\n");
        view.append("        }\n");
        view.append("    }\n");
        view.append("}\n");
        return view.toString();
    }

    /**
     * Generate settings view
     */
    private String generateSettingsView(iOSProjectRequest request) {
        StringBuilder view = new StringBuilder();
        view.append("import SwiftUI\n\n");
        view.append("struct SettingsView: View {\n");
        view.append("    @Environment(\\.dismiss) var dismiss\n");
        view.append("    @AppStorage(\"appTheme\") var appTheme: String = \"light\"\n");
        view.append("    @AppStorage(\"notificationsEnabled\") var notificationsEnabled = true\n");
        view.append("    \n");
        view.append("    var body: some View {\n");
        view.append("        NavigationStack {\n");
        view.append("            Form {\n");
        view.append("                Section(header: Text(\"Appearance\")) {\n");
        view.append("                    Picker(\"Theme\", selection: $appTheme) {\n");
        view.append("                        Text(\"Light\").tag(\"light\")\n");
        view.append("                        Text(\"Dark\").tag(\"dark\")\n");
        view.append("                        Text(\"Auto\").tag(\"auto\")\n");
        view.append("                    }\n");
        view.append("                }\n");
        view.append("                Section(header: Text(\"Notifications\")) {\n");
        view.append("                    Toggle(\"Enable Notifications\", isOn: $notificationsEnabled)\n");
        view.append("                }\n");
        view.append("                Section(header: Text(\"About\")) {\n");
        view.append("                    Text(\"Version 1.0.0\")\n");
        view.append("                }\n");
        view.append("            }\n");
        view.append("            .navigationTitle(\"Settings\")\n");
        view.append("            .navigationBarTitleDisplayMode(.inline)\n");
        view.append("            .toolbar {\n");
        view.append("                ToolbarItem(placement: .navigationBarTrailing) {\n");
        view.append("                    Button(\"Done\") { dismiss() }\n");
        view.append("                }\n");
        view.append("            }\n");
        view.append("        }\n");
        view.append("    }\n");
        view.append("}\n");
        return view.toString();
    }

    /**
     * Generate data models
     */
    private String generateDataModels(iOSProjectRequest request) {
        StringBuilder models = new StringBuilder();
        models.append("import Foundation\n\n");
        models.append("struct AppItem: Identifiable, Codable {\n");
        models.append("    let id: UUID\n");
        models.append("    let name: String\n");
        models.append("    let description: String\n");
        models.append("    var isActive: Bool\n");
        models.append("    let createdDate: Date\n");
        models.append("    var lastModified: Date\n");
        models.append("    var metadata: [String: String]\n");
        models.append("}\n\n");
        models.append("struct APIResponse: Codable {\n");
        models.append("    let success: Bool\n");
        models.append("    let data: [AppItem]?\n");
        models.append("    let error: String?\n");
        models.append("}\n");
        return models.toString();
    }

    /**
     * Generate view models
     */
    private String generateViewModels(iOSProjectRequest request) {
        StringBuilder viewModels = new StringBuilder();
        viewModels.append("import Foundation\n\n");
        viewModels.append("@MainActor\n");
        viewModels.append("class AppViewModel: ObservableObject {\n");
        viewModels.append("    @Published var items: [AppItem] = []\n");
        viewModels.append("    @Published var isLoading = false\n");
        viewModels.append("    @Published var errorMessage: String? = nil\n");
        viewModels.append("    \n");
        viewModels.append("    private let networkService = NetworkService()\n");
        viewModels.append("    \n");
        viewModels.append("    init() {\n");
        viewModels.append("        loadItems()\n");
        viewModels.append("    }\n");
        viewModels.append("    \n");
        viewModels.append("    func loadItems() {\n");
        viewModels.append("        isLoading = true\n");
        viewModels.append("        Task {\n");
        viewModels.append("            do {\n");
        viewModels.append("                self.items = try await networkService.fetchItems()\n");
        viewModels.append("                self.errorMessage = nil\n");
        viewModels.append("            } catch {\n");
        viewModels.append("                self.errorMessage = error.localizedDescription\n");
        viewModels.append("            }\n");
        viewModels.append("            self.isLoading = false\n");
        viewModels.append("        }\n");
        viewModels.append("    }\n");
        viewModels.append("    \n");
        viewModels.append("    func deleteItems(at offsets: IndexSet) {\n");
        viewModels.append("        items.remove(atOffsets: offsets)\n");
        viewModels.append("    }\n");
        viewModels.append("}\n");
        return viewModels.toString();
    }

    /**
     * Generate networking layer
     */
    private String generateNetworking(iOSProjectRequest request) {
        StringBuilder networking = new StringBuilder();
        networking.append("import Foundation\n\n");
        networking.append("class NetworkService {\n");
        networking.append("    private let baseURL = URL(string: \"https://api.example.com\")!\n");
        networking.append("    \n");
        networking.append("    func fetchItems() async throws -> [AppItem] {\n");
        networking.append("        let url = baseURL.appendingPathComponent(\"items\")\n");
        networking.append("        let (data, _) = try await URLSession.shared.data(from: url)\n");
        networking.append("        let response = try JSONDecoder().decode(APIResponse.self, from: data)\n");
        networking.append("        return response.data ?? []\n");
        networking.append("    }\n");
        networking.append("    \n");
        networking.append("    func createItem(_ item: AppItem) async throws -> AppItem {\n");
        networking.append("        let url = baseURL.appendingPathComponent(\"items\")\n");
        networking.append("        var request = URLRequest(url: url)\n");
        networking.append("        request.httpMethod = \"POST\"\n");
        networking.append("        request.setValue(\"application/json\", forHTTPHeaderField: \"Content-Type\")\n");
        networking.append("        request.httpBody = try JSONEncoder().encode(item)\n");
        networking.append("        \n");
        networking.append("        let (data, _) = try await URLSession.shared.data(for: request)\n");
        networking.append("        return try JSONDecoder().decode(AppItem.self, from: data)\n");
        networking.append("    }\n");
        networking.append("}\n");
        return networking.toString();
    }

    /**
     * Generate CoreData persistence
     */
    private String generateCoreData(iOSProjectRequest request) {
        StringBuilder coreData = new StringBuilder();
        coreData.append("import CoreData\n\n");
        coreData.append("class PersistenceController {\n");
        coreData.append("    static let shared = PersistenceController()\n");
        coreData.append("    \n");
        coreData.append("    let container: NSPersistentContainer\n");
        coreData.append("    \n");
        coreData.append("    init(inMemory: Bool = false) {\n");
        coreData.append("        container = NSPersistentContainer(name: \"").append(request.getProjectName()).append("\")\n");
        coreData.append("        \n");
        coreData.append("        if inMemory {\n");
        coreData.append("            container.persistentStoreDescriptions.first!.url = URL(fileURLWithPath: \"/dev/null\")\n");
        coreData.append("        }\n");
        coreData.append("        \n");
        coreData.append("        container.loadPersistentStores { _, error in\n");
        coreData.append("            if let error = error as NSError? {\n");
        coreData.append("                fatalError(\"Unresolved error \\(error), \\(error.userInfo)\")\n");
        coreData.append("            }\n");
        coreData.append("        }\n");
        coreData.append("    }\n");
        coreData.append("}\n");
        return coreData.toString();
    }

    /**
     * Generate build configuration
     */
    private String generateBuildConfig(iOSProjectRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("// Build Configuration for ").append(request.getProjectName()).append("\n");
        config.append("PRODUCT_NAME = ").append(request.getProjectName()).append("\n");
        config.append("MARKETING_VERSION = 1.0.0\n");
        config.append("CURRENT_PROJECT_VERSION = 1\n");
        config.append("IPHONEOS_DEPLOYMENT_TARGET = ").append(request.getTargetiOSVersion()).append("\n");
        config.append("SWIFT_VERSION = 5.9\n");
        config.append("SUPPORTED_PLATFORMS = iphoneos\n");
        config.append("CODE_SIGN_STYLE = Automatic\n");
        return config.toString();
    }

    /**
     * Generate Package.swift manifest
     */
    private String generatePackageManifest(iOSProjectRequest request) {
        StringBuilder manifest = new StringBuilder();
        manifest.append("// swift-tools-version:5.9\n");
        manifest.append("import PackageDescription\n\n");
        manifest.append("let package = Package(\n");
        manifest.append("    name: \"").append(request.getProjectName()).append("\",\n");
        manifest.append("    platforms: [\n");
        manifest.append("        .iOS(.v").append(request.getTargetiOSVersion()).append(")\n");
        manifest.append("    ],\n");
        manifest.append("    targets: [\n");
        manifest.append("        .target(\n");
        manifest.append("            name: \"").append(request.getProjectName()).append("\",\n");
        manifest.append("            dependencies: []\n");
        manifest.append("        ),\n");
        manifest.append("        .testTarget(\n");
        manifest.append("            name: \"").append(request.getProjectName()).append("Tests\",\n");
        manifest.append("            dependencies: [\"").append(request.getProjectName()).append("\"]\n");
        manifest.append("        )\n");
        manifest.append("    ]\n");
        manifest.append(")\n");
        return manifest.toString();
    }

    /**
     * Generate unit tests
     */
    private String generateUnitTests(iOSProjectRequest request) {
        StringBuilder tests = new StringBuilder();
        tests.append("import XCTest\n");
        tests.append("@testable import ").append(request.getProjectName()).append("\n\n");
        tests.append("final class AppViewModelTests: XCTestCase {\n");
        tests.append("    var viewModel: AppViewModel!\n");
        tests.append("    \n");
        tests.append("    override func setUp() {\n");
        tests.append("        super.setUp()\n");
        tests.append("        viewModel = AppViewModel()\n");
        tests.append("    }\n");
        tests.append("    \n");
        tests.append("    func testInitialState() {\n");
        tests.append("        XCTAssertTrue(viewModel.items.isEmpty)\n");
        tests.append("        XCTAssertNil(viewModel.errorMessage)\n");
        tests.append("    }\n");
        tests.append("    \n");
        tests.append("    func testDeleteItems() {\n");
        tests.append("        let item = AppItem(id: UUID(), name: \"Test\", description: \"Test\", isActive: true, createdDate: Date(), lastModified: Date(), metadata: [:])\n");
        tests.append("        viewModel.items = [item]\n");
        tests.append("        viewModel.deleteItems(at: IndexSet(integer: 0))\n");
        tests.append("        XCTAssertTrue(viewModel.items.isEmpty)\n");
        tests.append("    }\n");
        tests.append("}\n");
        return tests.toString();
    }

    /**
     * Generate UI tests
     */
    private String generateUITests(iOSProjectRequest request) {
        StringBuilder tests = new StringBuilder();
        tests.append("import XCTest\n\n");
        tests.append("final class ").append(request.getProjectName()).append("UITests: XCTestCase {\n");
        tests.append("    override func setUpWithError() throws {\n");
        tests.append("        continueAfterFailure = false\n");
        tests.append("    }\n");
        tests.append("    \n");
        tests.append("    func testOpenApplication() throws {\n");
        tests.append("        let app = XCUIApplication()\n");
        tests.append("        app.launch()\n");
        tests.append("        XCTAssertTrue(app.exists)\n");
        tests.append("    }\n");
        tests.append("    \n");
        tests.append("    func testNavigationFlow() throws {\n");
        tests.append("        let app = XCUIApplication()\n");
        tests.append("        app.launch()\n");
        tests.append("        let settingsButton = app.navigationBars.buttons[\"gear\"]\n");
        tests.append("        XCTAssertTrue(settingsButton.exists)\n");
        tests.append("        settingsButton.tap()\n");
        tests.append("    }\n");
        tests.append("}\n");
        return tests.toString();
    }

    /**
     * Estimate total lines of code
     */
    private int estimateLinesOfCode(iOSProjectOutput output) {
        int total = 0;
        total += output.appView.split("\n").length;
        total += output.contentView.split("\n").length;
        total += output.detailView.split("\n").length;
        total += output.settingsView.split("\n").length;
        total += output.dataModels.split("\n").length;
        total += output.viewModels.split("\n").length;
        total += output.networkingCode.split("\n").length;
        if (output.persistenceCode != null) total += output.persistenceCode.split("\n").length;
        total += output.buildConfiguration.split("\n").length;
        total += output.packageManifest.split("\n").length;
        total += output.unitTests.split("\n").length;
        total += output.uiTests.split("\n").length;
        return total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class iOSProjectRequest {
        private String projectName;
        private String targetiOSVersion;
        private boolean persistenceEnabled;
        private List<String> requiredDependencies;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class iOSProjectOutput {
        private LocalDateTime timestamp;
        private String projectName;
        private String targetiOSVersion;
        private String appView;
        private String contentView;
        private String detailView;
        private String settingsView;
        private String dataModels;
        private String viewModels;
        private String networkingCode;
        private String persistenceCode;
        private String buildConfiguration;
        private String packageManifest;
        private String unitTests;
        private String uiTests;
        private boolean generationSuccess;
        private String statusMessage;
        private int linesOfCode;
    }
}
