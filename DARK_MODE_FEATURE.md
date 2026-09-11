# Dark Mode Feature Implementation

## Overview
This document describes the Dark Mode toggle feature that has been added to the esProc IDE application.

## Feature Description
A new dark/light mode toggle feature has been implemented in the esProc IDE, allowing users to switch between dark and light themes easily. The theme preference is persisted in the configuration file, so the selected theme is restored when the application restarts.

## Changes Made

### 1. **GCMenu.java** - Added Menu Constants
**File:** `ide/main/java/com/scudata/ide/vdb/menu/GCMenu.java`

Added new constants for the theme toggle menu item and button:
```java
public static final String TOOLS_TOGGLE_THEME = "tools.toggletheme"; // 切换主题(Dark/Light)
public static final short iTOOLS_TOGGLE_THEME = 460; // 切换主题
```

### 2. **LNFManager.java** - Extended Theme Support
**File:** `ide/main/java/com/scudata/ide/vdb/commonvdb/LNFManager.java`

- Added `LNF_DARK` constant for dark mode
- Updated `listLNFCode()` and `listLNFDisp()` methods to include dark mode option
- Implemented `applyDarkMode()` method that applies dark color scheme to all UI components:
  - Background colors changed to dark gray (#323232)
  - Text colors changed to light gray (#DCDCDC)
  - Covers: Panel, Menu, Button, TextField, Tree, Table, ComboBox, Dialog, List, ScrollPane, etc.
- Implemented `applyLightMode()` method to reset to default light theme

### 3. **ConfigOptions.java** - Added Dark Mode Preference
**File:** `ide/main/java/com/scudata/ide/vdb/config/ConfigOptions.java`

- Added `bDarkMode` boolean field to store dark mode preference
- Updated `putOptions()` to include dark mode in saved options
- Updated `loadOption()` method to load dark mode preference from configuration
- The preference is persisted and restored on application startup

### 4. **MenuVDB.java** - Added Toggle Menu Item
**File:** `ide/main/java/com/scudata/ide/vdb/menu/MenuVDB.java`

Added dark mode toggle to the Tools menu:
```java
menu.add(newMenuItem(GCMenu.iTOOLS_TOGGLE_THEME, GCMenu.TOOLS_TOGGLE_THEME, 'D', Boolean.TRUE, true));
```

Location: Tools menu (keyboard shortcut: Ctrl+D)

### 5. **ToolbarVDB.java** - Added Toggle Button
**File:** `ide/main/java/com/scudata/ide/vdb/menu/ToolbarVDB.java`

Added a toolbar button with separator for the dark mode toggle:
```java
addSeparator();
add(getButton(GCMenu.iTOOLS_TOGGLE_THEME, GCMenu.TOOLS_TOGGLE_THEME));
```

### 6. **VDB.java** - Implemented Toggle Logic
**File:** `ide/main/java/com/scudata/ide/vdb/VDB.java`

- Added imports for `UIManager` and `LNFManager`
- Modified `init()` method to apply dark mode if it was previously enabled
- Added `toggleDarkMode()` method that:
  - Toggles the `bDarkMode` flag
  - Saves the configuration
  - Applies the corresponding color scheme
  - Updates all UI components
  - Shows a confirmation dialog
- Added case for `iTOOLS_TOGGLE_THEME` in `executeCmd()` method

### 7. **pom.xml** - Added JAXB Dependencies
**File:** `pom.xml`

Added Java 9+ compatibility dependencies:
```xml
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>2.3.1</version>
</dependency>
```

## How to Use

### Via Menu
1. Click on **Tools** menu
2. Select **Toggle Theme** (or press Ctrl+D)
3. The theme will switch between Dark and Light modes
4. A confirmation dialog will appear
5. Restart the application for full effect (optional - UI will update immediately)

### Via Toolbar
1. Look for the theme toggle button in the toolbar (appears after a separator on the right)
2. Click the button to toggle between Dark and Light modes
3. Confirmation dialog will appear
4. Theme preference is automatically saved

## Theme Details

### Dark Mode Colors
- **Background:** RGB(50, 50, 50) - Dark gray
- **Text/Foreground:** RGB(220, 220, 220) - Light gray
- **Grid Color:** RGB(80, 80, 80) - Medium gray
- **Menu Bar:** RGB(40, 40, 40) - Very dark gray

### Affected Components
- Control/Panel backgrounds
- Menu and menu items
- Buttons and ComboBoxes
- Text areas and text fields
- Trees and Tables
- Dialogs and option panes
- List and Scroll panes
- Separators

## Configuration File
The dark mode preference is stored in the configuration file with the key `bDarkMode` with values:
- `true` - Dark mode enabled
- `false` - Light mode enabled (default)

## Technical Implementation

### Theme Toggle Flow
```
User clicks Toggle → toggleDarkMode() → 
  Toggle bDarkMode flag → 
  Save configuration → 
  Apply color scheme (applyDarkMode/applyLightMode) → 
  Update UI components (SwingUtilities.updateComponentTreeUI) → 
  Show confirmation dialog
```

### Persistence
The theme preference is saved to the configuration XML file and automatically loaded when the application starts. The `init()` method checks the `bDarkMode` flag and applies the dark mode colors if needed.

## Future Enhancements

Possible improvements for future versions:
1. **System Theme Detection:** Auto-detect the system's dark/light mode preference
2. **Theme Customization:** Allow users to customize colors for dark/light modes
3. **Additional Themes:** Add more theme options (e.g., high contrast, custom color schemes)
4. **Smooth Transitions:** Animate the theme transition for better UX
5. **Per-Component Styling:** Fine-tune colors for specific components
6. **Icon Theme Switching:** Change icon sets based on theme selection

## Testing Notes

To verify the dark mode feature works correctly:

1. **First Toggle:** Start the application in light mode, click the toggle → should switch to dark mode
2. **Persistence:** Close and reopen the application → should remain in dark mode
3. **Second Toggle:** Click the toggle again → should switch back to light mode
4. **UI Updates:** Check that all visible components update their colors
5. **Menu Access:** Verify both menu and toolbar button work identically

## Files Modified Summary

| File | Changes |
|------|---------|
| `GCMenu.java` | Added TOOLS_TOGGLE_THEME constants |
| `LNFManager.java` | Added LNF_DARK, applyDarkMode(), applyLightMode() |
| `ConfigOptions.java` | Added bDarkMode field and configuration handling |
| `MenuVDB.java` | Added theme toggle menu item |
| `ToolbarVDB.java` | Added theme toggle button |
| `VDB.java` | Added toggleDarkMode() logic and command handling |
| `pom.xml` | Added JAXB dependencies for Java 9+ compatibility |

## Compatibility
- **Java Version:** 8+ (tested with Java 20)
- **Swing Components:** Full compatibility with standard Swing components
- **Custom Components:** May need additional styling for custom Swing components not in the standard library

## Notes
- The feature uses standard Swing `UIManager` for theme switching
- Color values are hardcoded; can be moved to configuration file for more flexibility
- The implementation supports switching themes at runtime without restarting the application
- Restart is recommended to ensure all components display correctly with the new theme
