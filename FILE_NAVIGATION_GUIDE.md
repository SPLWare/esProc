# Dark Mode Implementation - File Navigation Guide

This guide helps you locate and understand each code change in the esProc repository.

## File-by-File Navigation

### 1. 📄 GCMenu.java
**Location**: `ide/main/java/com/scudata/ide/vdb/menu/GCMenu.java`

**What's New**:
```
Lines 67-68  →  TOOLS_TOGGLE_THEME constants
Line 67      →  public static final String TOOLS_TOGGLE_THEME = "tools.toggletheme";
Line 68      →  public static final short iTOOLS_TOGGLE_THEME = 460;
```

**Why**: Defines the menu command ID and display text key for the theme toggle feature

**Jump To**:
- Search: `iTOOLS_OPTION` (nearby constant, line ~65)
- Add new lines after that constant definition

---

### 2. 📄 LNFManager.java
**Location**: `ide/main/java/com/scudata/ide/vdb/commonvdb/LNFManager.java`

**What's New**:

| Change | Line | What It Does |
|--------|------|-------------|
| `LNF_DARK = 4` | 18 | New dark mode constant |
| Updated `listLNFCode()` | 26 | Add dark mode to list |
| Updated `listLNFDisp()` | 35 | Add "Dark" display name |
| Updated `getLookAndFeelName()` | 59 | Handle dark mode case |
| `applyDarkMode()` | NEW | Apply dark colors |
| `applyLightMode()` | NEW | Reset light colors |

**Key Methods**:
```
Line 56   → getLookAndFeelName() - Add case LNF_DARK
Line 69   → applyDarkMode() - NEW METHOD (50 lines)
Line 95   → applyLightMode() - NEW METHOD (10 lines)
```

**Color Definitions** (in applyDarkMode):
- Control backgrounds: RGB(50, 50, 50)
- Text colors: RGB(220, 220, 220)
- Menu bar: RGB(40, 40, 40)
- Grid colors: RGB(80, 80, 80)

**Required Import**:
```
Add: import javax.swing.plaf.ColorUIResource;
```

---

### 3. 📄 ConfigOptions.java
**Location**: `ide/main/java/com/scudata/ide/vdb/config/ConfigOptions.java`

**What's New**:

| Line | Change | Purpose |
|------|--------|---------|
| 24 | `public static Boolean bDarkMode = Boolean.FALSE;` | Dark mode flag |
| 53 | `options.put("bDarkMode", bDarkMode);` | Save option |
| 95 | Handle `bDarkMode` in loadOption() | Load option |

**Find Locations**:
```
Line 20  → bAutoOpen (nearby field)
Line 53  → options.put("iLookAndFeel", ...) (add line after this)
Line 94  → } else if (option.equalsIgnoreCase("bHoldConsole")) {
         → Add bDarkMode check after this
```

**Configuration File Impact**:
- Saved to: `config/userconfig.xml`
- Key: `<bDarkMode>true</bDarkMode>` or `<bDarkMode>false</bDarkMode>`

---

### 4. 📄 MenuVDB.java
**Location**: `ide/main/java/com/scudata/ide/vdb/menu/MenuVDB.java`

**What's New**:
```
Line 53  → menu.add(newMenuItem(GCMenu.iTOOLS_TOGGLE_THEME, 
                                 GCMenu.TOOLS_TOGGLE_THEME, 'D', Boolean.TRUE, true));
```

**Find Location**:
```
Search for: "iTOOLS_OPTION"
Add the new line BEFORE that line
```

**Full Context**:
```java
// 工具菜单
menu = newMenu(GCMenu.iTOOLS, GCMenu.TOOLS, 'T', true);
menu.add(newMenuItem(GCMenu.iTOOLS_TOGGLE_THEME, GCMenu.TOOLS_TOGGLE_THEME, 'D', Boolean.TRUE, true));  // ← NEW
menu.add(newMenuItem(GCMenu.iTOOLS_OPTION, GCMenu.TOOLS_OPTION, 'O', Boolean.FALSE, true));
add(menu);
```

---

### 5. 📄 ToolbarVDB.java
**Location**: `ide/main/java/com/scudata/ide/vdb/menu/ToolbarVDB.java`

**What's New**:
```
Line 12  → addSeparator();
Line 13  → add(getButton(GCMenu.iTOOLS_TOGGLE_THEME, GCMenu.TOOLS_TOGGLE_THEME));
```

**Find Location**:
```
Search for: "iCONN_CONFIG"
Add separator and button after that line
```

**Full Context**:
```java
public ToolbarVDB() {
    super();
    add(getButton(GCMenu.iCONN_NEW, GCMenu.CONN_NEW));
    add(getButton(GCMenu.iCONN_OPEN, GCMenu.CONN_OPEN));
    add(getButton(GCMenu.iCONN_SAVE, GCMenu.CONN_SAVE));
    add(getButton(GCMenu.iCONN_CLOSE, GCMenu.CONN_CLOSE));
    add(getButton(GCMenu.iCONN_CONFIG, GCMenu.CONN_CONFIG));
    addSeparator();  // ← NEW
    add(getButton(GCMenu.iTOOLS_TOGGLE_THEME, GCMenu.TOOLS_TOGGLE_THEME));  // ← NEW
}
```

---

### 6. 📄 VDB.java
**Location**: `ide/main/java/com/scudata/ide/vdb/VDB.java`

**What's New**:

| Section | Line | Change |
|---------|------|--------|
| Imports | Top | Add `UIManager`, `LNFManager` imports |
| init() | ~87 | Apply dark mode after loading config |
| New Method | Before executeCmd | Add toggleDarkMode() (20 lines) |
| executeCmd() | ~410 | Add case for iTOOLS_TOGGLE_THEME |

**Import Additions**:
```java
import javax.swing.UIManager;
import com.scudata.ide.vdb.commonvdb.LNFManager;
```

**Find Location for init() Update**:
```
Search: "ConfigOptions.load();"
Add dark mode check AFTER that try-catch block
```

**Find Location for toggleDarkMode() Method**:
```
Search: "public void executeCmd(short cmdId) {"
Add toggleDarkMode() method BEFORE this method
```

**Find Location for executeCmd() Case**:
```
Search: "case GCMenu.iTOOLS_OPTION:"
Add new case BEFORE this line:
    case GCMenu.iTOOLS_TOGGLE_THEME:
        toggleDarkMode();
        return;
```

**Full toggleDarkMode Method**:
```java
private void toggleDarkMode() {
    try {
        ConfigOptions.bDarkMode = !ConfigOptions.bDarkMode;
        ConfigOptions.save();
        
        if (ConfigOptions.bDarkMode) {
            LNFManager.applyDarkMode();
        } else {
            LNFManager.applyLightMode();
        }
        
        SwingUtilities.updateComponentTreeUI(this);
        
        String message = ConfigOptions.bDarkMode ? 
            "Dark mode enabled. Please restart the application for full effect." :
            "Light mode enabled. Please restart the application for full effect.";
        JOptionPane.showMessageDialog(this, message, "Theme Changed", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception e) {
        GM.showException(GV.appFrame, e);
    }
}
```

---

### 7. 📄 pom.xml
**Location**: `pom.xml` (project root)

**What's New**:
```
Lines 60-70  →  Add JAXB dependencies
```

**Find Location**:
```
Search: <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        
Add before these lines (for Java 9+ compatibility)
```

**Dependencies to Add**:
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

---

## Quick Reference Table

| File | Location in File | What Changed | Lines Added |
|------|-----------------|--------------|------------|
| **GCMenu.java** | TOOLS section | 2 constants | 2 |
| **LNFManager.java** | Various | 1 const, 5 methods | ~80 |
| **ConfigOptions.java** | Class fields & methods | 1 field, 2 usages | 3 |
| **MenuVDB.java** | Constructor (Tools menu) | 1 menu item | 1 |
| **ToolbarVDB.java** | Constructor (toolbar) | 1 separator + 1 button | 2 |
| **VDB.java** | Imports, init, methods | 2 imports, 1 toggle, 1 case | ~30 |
| **pom.xml** | Dependencies section | 2 dependencies | 8 |
| **TOTAL** | | | ~126 |

---

## Search Strings for Navigation

Use these strings to quickly navigate to change locations:

```
// In GCMenu.java
Search: "iTOOLS_OPTION"

// In LNFManager.java
Search: "getLookAndFeelName"
Search: "isNimbusEnabled"

// In ConfigOptions.java
Search: "bAutoOpen"
Search: "bHoldConsole"

// In MenuVDB.java
Search: "iTOOLS_OPTION"

// In ToolbarVDB.java
Search: "iCONN_CONFIG"

// In VDB.java
Search: "ConfigOptions.load"
Search: "executeCmd"

// In pom.xml
Search: "httpclient"
```

---

## Implementation Order (If Doing Manually)

If implementing changes in order, follow this sequence:

1. ✅ **pom.xml** - Add dependencies first (fixes compilation)
2. ✅ **GCMenu.java** - Define menu constants
3. ✅ **LNFManager.java** - Implement theme logic
4. ✅ **ConfigOptions.java** - Add configuration storage
5. ✅ **VDB.java** - Add toggle logic
6. ✅ **MenuVDB.java** - Add menu item
7. ✅ **ToolbarVDB.java** - Add toolbar button

This order ensures dependencies are satisfied at each step.

---

## Visual Summary

### Dark Mode Feature Architecture Map

```
┌─ GCMenu.java
│  └─ Defines: TOOLS_TOGGLE_THEME, iTOOLS_TOGGLE_THEME
│
├─ MenuVDB.java ──┐
│                 ├─→ Calls: getButton() from ToolbarFactory
├─ ToolbarVDB.java┤   Calls: newMenuItem() from MenuFactory
│                 │
│                 └─→ Both map to: VDB.executeCmd(iTOOLS_TOGGLE_THEME)
│
├─ VDB.java
│  ├─ executeCmd() ──→ case iTOOLS_TOGGLE_THEME: toggleDarkMode()
│  ├─ toggleDarkMode() ──→ Toggle config
│  │                  ├─→ ConfigOptions.save()
│  │                  └─→ LNFManager.apply*()
│  └─ init() ────────→ Apply saved theme on startup
│
├─ ConfigOptions.java
│  ├─ bDarkMode field (stores preference)
│  ├─ save() (writes to XML)
│  └─ load() (reads from XML)
│
├─ LNFManager.java
│  ├─ applyDarkMode() (sets dark colors)
│  └─ applyLightMode() (resets to light)
│
└─ pom.xml
   └─ JAXB dependencies (for Java 9+)
```

---

## Common Navigation Patterns

### To Add a Menu Item:
1. Open GCMenu.java → Add constant
2. Open MenuVDB.java → Add menu.add(newMenuItem(...))
3. That's it! MenuFactory handles the rest

### To Add a Toolbar Button:
1. Same constants from GCMenu.java are reused
2. Open ToolbarVDB.java → Add add(getButton(...))
3. Done!

### To Handle a Menu/Toolbar Click:
1. Open VDB.java → Add case in executeCmd()
2. Implement the logic (like toggleDarkMode)
3. Done!

### To Add Configuration:
1. Open ConfigOptions.java → Add public static field
2. Add to putOptions()
3. Add to loadOption()
4. Configuration is automatically saved/loaded

---

**End of Navigation Guide**

Use this guide to quickly locate and understand each part of the dark mode implementation!
