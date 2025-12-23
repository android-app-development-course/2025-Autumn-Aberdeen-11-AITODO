# Project API & Module Map

## 1. Data Layer (Models & Repos)
| Class/File | Description | Key Fields/Methods |
| :--- | :--- | :--- |
| `TaskEntity` | Core data model for a Task | `id`, `title`, `deadline`, `reminder`, `repeatMode`, `createdAt`, `isCompleted`, `isPinned`, `isSelected` |
| `TaskFilter` | Enum for filtering tasks | `ALL`, `CURRENT`, `PINNED`, `HISTORY` |
| `TaskRepository` | (Interface) Data source | *Pending Implementation* |
| `AIProviderConfig` | AI Provider Configuration | `id`, `name`, `baseUrl`, `apiKey`, `selectedModelId`, `models` |
| `ChatSettings` | Chat Session Settings | `systemPrompt`, `contextLimit`, `temperature` |
| `TaskDraft` | Intermediate model for AI-proposed tasks | `title`, `deadline`, `reminder`, `repeatMode`, `toTaskEntity()` |
| `AISettingsRepository` | Manages AI Settings persistence | `providers` (Flow), `chatSettings` (Flow), `saveProviders()`, `saveChatSettings()` |
| `OpenAIService` | Retrofit interface for AI API | `getModels()`, `chatCompletion()` |

## 2. UI Layer (Screens & Components)
| Composable | Associated ViewModel | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `MainScreen` | N/A | App entry, holds `Scaffold` & `BottomNavigation` | `taskListViewModel` |
| `TaskListScreen` | `TaskListViewModel` | Main Task List Screen. Hoists `isSearchActive` state. | `viewModel` |
| `AIChatScreen` | `AIChatViewModel` | Main Chat Screen with Messages & Drafts | `viewModel`, `taskListViewModel` |
| `TaskItem` | N/A | Reusable Task Card (Swipeable) | `task`, `onChecked`, `onClick`, `onEditClick`, `onPin`, `onSelect` |
| `TopBarArea` | N/A | Search, Add Task & Sort Controls. Stateless. | `uiState`, `isSearchActive`, `onSearchActiveChange`, callbacks |
| `AddTaskDialog` | N/A | Compact dialog for creating/editing tasks | `targetTask`, `onDismiss`, `onConfirm`, `onDelete` |
| `SettingsScreen` | `SettingsViewModel` | Main Settings Hub with nested navigation | `N/A` |
| `ProviderListScreen`| `SettingsViewModel` | Lists saved AI providers | `viewModel`, `onNavigateToEdit`, `onBack` |
| `ProviderEditScreen`| `SettingsViewModel` | Add/Edit Provider & Fetch Models | `viewModel`, `providerId`, `onBack` |

## 3. Logic Layer (ViewModels)
| ViewModel | Responsibility | Public State (StateFlow) | Public Actions |
| :--- | :--- | :--- | :--- |
| `TaskListViewModel` | Business logic for Task List | `uiState: TaskListUiState` | `onFilterChanged`, `onSearchQueryChanged`, `onSortOptionChanged`, `toggleSortDirection`, `toggleComplete`, `togglePin`, `toggleSelection`, `onAddTaskClick`, `onEditTaskRequest`, `onSaveTask`, `onDeleteTask`, `onDismissEditor` |
| `SettingsViewModel` | AI Provider & Settings Management | `providers`, `fetchState` | `addOrUpdateProvider`, `deleteProvider`, `fetchModels` |
| `AIChatViewModel` | AI Chat & Task Proposal Logic | `uiState: AIChatUiState` | `onInputChange`, `sendMessage`, `onProviderSelected`, `onTaskAccepted`, `ignoreTaskDraft`, `updateChatSettings` |

## 4. Theme & Resources
| File | Purpose |
| :--- | :--- |
| `Dimens.kt` | Centralized dimensions (Padding, Icon Sizes) |
| `Color.kt` | Color palettes (Light/Dark) |
| `Theme.kt` | MaterialTheme setup |
