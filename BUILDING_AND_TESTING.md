# Building and Testing the Dark Mode Feature

## Prerequisites
- Java 8 or higher (tested with Java 20)
- Maven 3.8.9 or higher
- Git (already cloned)

## Build Instructions

### Step 1: Install Maven (if not already installed)
```powershell
# Maven will be installed to C:\Users\<username>\.maven\maven-3.9.11\bin
```

### Step 2: Navigate to Project Directory
```powershell
cd "c:\Aadya College Stuff\Git\EsProc-contribution\esProc"
```

### Step 3: Compile the Project
```powershell
# Add Maven to PATH
$env:PATH += ";C:\Users\aadya\.maven\maven-3.9.11\bin"

# Run Maven compile
mvn clean compile
```

**Note:** The first compilation may take several minutes as it downloads all dependencies and compiles 1200+ source files.

### Step 4: Package the Application
```powershell
mvn package
```

This creates a JAR file in the `target` directory.

## Running the Application

### From IDE
If using Eclipse or another IDE with Maven support:
1. Right-click on project → Run As → Java Application
2. Select the main class (typically in the IDE or SPL module)

### From Command Line
```powershell
java -jar target/esproc-20250801.jar
```

Or navigate to the `bin` directory:
```powershell
cd bin
./startup.bat  # On Windows
./startup.sh   # On Linux/Mac
```

## Testing the Dark Mode Feature

### Test Case 1: Enable Dark Mode
**Steps:**
1. Start the application in light mode (default)
2. Go to **Tools** → **Toggle Theme** (or press Ctrl+D)
3. A dialog should appear saying "Dark mode enabled..."
4. Verify that all UI components change to dark colors

**Expected Result:** 
- All backgrounds change to dark gray
- All text changes to light gray
- Menu bar, buttons, text fields, trees, and tables are all darkened

### Test Case 2: Toggle Button
**Steps:**
1. Look for a button in the toolbar (should be after other buttons with a separator)
2. Click the dark mode toggle button
3. Application should switch to dark mode

**Expected Result:**
- Same as Test Case 1
- Both menu and toolbar button should work identically

### Test Case 3: Persistence
**Steps:**
1. Enable dark mode (Tools → Toggle Theme)
2. Close the application
3. Restart the application
4. Verify that the application starts in dark mode

**Expected Result:**
- Application should remember the dark mode preference
- No action needed - should apply automatically on startup

### Test Case 4: Toggle Back to Light
**Steps:**
1. Application is in dark mode (from Test Case 3)
2. Go to Tools → Toggle Theme again
3. Application should switch back to light mode
4. Close and reopen to verify light mode is saved

**Expected Result:**
- Application switches back to light mode
- Preference is saved and restored on next startup

### Test Case 5: Menu Item Access
**Steps:**
1. Click on Tools menu
2. Verify "Toggle Theme" menu item is visible
3. Verify keyboard shortcut (Ctrl+D) works

**Expected Result:**
- Menu item is present and enabled
- Ctrl+D keyboard shortcut successfully triggers the toggle

## Troubleshooting

### Issue: Maven not found
**Solution:**
- Add Maven bin directory to system PATH or use full path to mvn.exe
- Verify Maven installation: `mvn --version`

### Issue: Compilation fails with "package does not exist"
**Possible causes:**
- Missing JAXB dependencies (fixed in pom.xml)
- Wrong Java version (need Java 8+)
**Solution:**
- Ensure pom.xml has JAXB dependencies
- Update Java version: `java -version`

### Issue: File locks during clean
**Solution:**
- Close any IDE windows accessing the project
- Don't run clean if files are still locked
- Use `mvn compile` instead of `mvn clean compile`

### Issue: Theme doesn't persist after restart
**Solution:**
- Check that config directory exists: `config/userconfig.xml`
- Verify write permissions to config directory
- Ensure ConfigOptions.save() is being called

### Issue: UI components not updating in dark mode
**Solution:**
- Restart the application for full effect (not just theme toggle)
- Some custom components may need additional styling
- Check console for any error messages

## Development Notes

### Adding More Colors to Dark Mode
To customize dark mode colors, edit `LNFManager.applyDarkMode()`:

```java
public static void applyDarkMode() {
    UIManager.put("Control", new ColorUIResource(50, 50, 50));
    // Modify RGB values as needed
}
```

### Adding More UI Components to Theme
To add more components to the dark theme, add new `UIManager.put()` calls in `applyDarkMode()`:

```java
UIManager.put("YourComponent.background", new ColorUIResource(50, 50, 50));
UIManager.put("YourComponent.foreground", new ColorUIResource(220, 220, 220));
```

### Loading Theme from Configuration
The theme is automatically loaded from config file on startup. To verify:
1. Look at `ConfigOptions.load()` method
2. Verify `bDarkMode` is loaded from XML
3. Check `VDB.init()` applies theme after loading config

## Performance Considerations
- Theme switching uses `SwingUtilities.updateComponentTreeUI()` which updates all components
- This operation is performed on the EDT (Event Dispatch Thread)
- For large UIs, the update may take a moment
- Consider using a progress dialog for very large applications

## Future Testing
Once the dark mode is stable:
1. Test with different screen resolutions
2. Test with different Look and Feels
3. Test with custom Swing components
4. Test accessibility with screen readers
5. Perform usability testing with actual users

## Build Artifacts
After successful build:
- **JAR file:** `target/esproc-20250801.jar`
- **Classes:** `target/classes/`
- **Dependencies:** `target/dependency/` (if using assembly)

## Cleaning Build
To remove all build artifacts:
```powershell
mvn clean
```

To remove only compiled classes:
```powershell
Remove-Item -Recurse -Force target/classes
```
