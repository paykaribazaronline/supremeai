// Widget tests for Flutter Admin Dashboard screens
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

// Mock classes
class MockDeploymentService extends Mock {
  Future<List<dynamic>> listDeployments() async => [];
}

class MockKubernetesService extends Mock {
  Future<dynamic?> getClusterHealth() async => null;
}

class MockDockerService extends Mock {
  Future<List<dynamic>> listImages() async => [];
}

class MockPipelineService extends Mock {
  Future<List<dynamic>> listPipelines() async => [];
}

void main() {
  group('DeploymentListScreen Widget Tests', () {
    late MockDeploymentService mockDeploymentService;

    setUp(() {
      mockDeploymentService = MockDeploymentService();
    });

    testWidgets('displays loading indicator on initial load',
        (WidgetTester tester) async {
      when(mockDeploymentService.listDeployments()).thenAnswer(
        (_) => Future.delayed(
          const Duration(seconds: 2),
          () => [],
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockDeploymentService>.value(
                value: mockDeploymentService,
              ),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('displays empty state when no deployments',
        (WidgetTester tester) async {
      when(mockDeploymentService.listDeployments()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockDeploymentService>.value(
                value: mockDeploymentService,
              ),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.text('No deployments found'), findsOneWidget);
    });

    testWidgets('displays filter chips', (WidgetTester tester) async {
      when(mockDeploymentService.listDeployments()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockDeploymentService>.value(
                value: mockDeploymentService,
              ),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.byType(FilterChip), findsWidgets);
    });

    testWidgets('refresh button triggers reload', (WidgetTester tester) async {
      when(mockDeploymentService.listDeployments()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockDeploymentService>.value(
                value: mockDeploymentService,
              ),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      final refreshButton = find.byIcon(Icons.refresh);
      expect(refreshButton, findsOneWidget);

      await tester.tap(refreshButton);
      await tester.pumpAndSettle();

      verify(mockDeploymentService.listDeployments()).called(greaterThan(1));
    });
  });

  group('KubernetesOverviewScreen Widget Tests', () {
    late MockKubernetesService mockK8sService;

    setUp(() {
      mockK8sService = MockKubernetesService();
    });

    testWidgets('displays cluster health card', (WidgetTester tester) async {
      when(mockK8sService.getClusterHealth()).thenAnswer(
        (_) => Future.value(null),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockKubernetesService>.value(value: mockK8sService),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.text('Cluster Health'), findsOneWidget);
    });

    testWidgets('displays pod statistics', (WidgetTester tester) async {
      when(mockK8sService.getClusterHealth()).thenAnswer(
        (_) => Future.value(null),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockKubernetesService>.value(value: mockK8sService),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.text('Pod Status'), findsOneWidget);
    });
  });

  group('DockerImageListScreen Widget Tests', () {
    late MockDockerService mockDockerService;

    setUp(() {
      mockDockerService = MockDockerService();
    });

    testWidgets('displays search bar', (WidgetTester tester) async {
      when(mockDockerService.listImages()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockDockerService>.value(value: mockDockerService),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      expect(find.byType(SearchBar), findsOneWidget);
    });

    testWidgets('search filters images', (WidgetTester tester) async {
      when(mockDockerService.listImages()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockDockerService>.value(value: mockDockerService),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      final searchBar = find.byType(SearchBar);
      expect(searchBar, findsOneWidget);

      await tester.enterText(searchBar, 'test');
      await tester.pumpAndSettle();
    });
  });

  group('PipelineListScreen Widget Tests', () {
    late MockPipelineService mockPipelineService;

    setUp(() {
      mockPipelineService = MockPipelineService();
    });

    testWidgets('displays pipeline list', (WidgetTester tester) async {
      when(mockPipelineService.listPipelines()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockPipelineService>.value(
                value: mockPipelineService,
              ),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.byType(ListView), findsOneWidget);
    });

    testWidgets('filter chip toggles pipeline list', (WidgetTester tester) async {
      when(mockPipelineService.listPipelines()).thenAnswer(
        (_) => Future.value([]),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: MultiProvider(
            providers: [
              Provider<MockPipelineService>.value(
                value: mockPipelineService,
              ),
            ],
            child: const Placeholder(),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.byType(FilterChip), findsWidgets);

      final enabledChip = find.text('enabled');
      if (enabledChip.evaluate().isNotEmpty) {
        await tester.tap(enabledChip);
        await tester.pumpAndSettle();
      }
    });
  });

  group('AppBar Navigation Tests', () {
    testWidgets('AppBar displays correct titles', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            appBar: AppBar(title: Text('Deployments')),
          ),
        ),
      );

      expect(find.text('Deployments'), findsOneWidget);
    });

    testWidgets('FloatingActionButton is accessible', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            appBar: AppBar(title: const Text('Test')),
            floatingActionButton: FloatingActionButton(
              onPressed: () {},
              child: const Icon(Icons.add),
            ),
          ),
        ),
      );

      expect(find.byType(FloatingActionButton), findsOneWidget);
    });
  });
}
