"""Cleanup backup directories from Phoenix folder"""
import os
import sys
import subprocess
import shutil
from pathlib import Path

# Set UTF-8 encoding
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

def remove_directory_robust(path):
    """Remove directory using multiple methods"""
    path = Path(path)
    if not path.exists():
        return True
    
    print(f"  Removing: {path.name}...")
    
    # Method 1: Try shutil.rmtree
    try:
        shutil.rmtree(path)
        if not path.exists():
            print(f"    [OK] Removed via shutil")
            return True
    except Exception as e:
        print(f"    [WARNING] shutil failed: {e}")
    
    # Method 2: Try robocopy (Windows)
    if sys.platform == 'win32':
        try:
            temp_empty = path.parent / f"{path.name}_empty_temp_{os.getpid()}"
            temp_empty.mkdir(exist_ok=True)
            
            result = subprocess.run(
                ['robocopy', str(temp_empty), str(path), '/MIR', '/R:0', '/W:0', '/NFL', '/NDL', '/NJH', '/NJS'],
                capture_output=True,
                timeout=30
            )
            
            # Cleanup temp
            try:
                shutil.rmtree(temp_empty, ignore_errors=True)
            except:
                pass
            
            # Try to remove again
            try:
                shutil.rmtree(path, ignore_errors=True)
            except:
                pass
            
            if not path.exists():
                print(f"    [OK] Removed via robocopy")
                return True
        except Exception as e:
            print(f"    [WARNING] robocopy failed: {e}")
    
    # Method 3: Try PowerShell Remove-Item
    if sys.platform == 'win32':
        try:
            result = subprocess.run(
                ['powershell', '-Command', f'Remove-Item -LiteralPath "{path}" -Recurse -Force -ErrorAction SilentlyContinue'],
                timeout=30,
                capture_output=True
            )
            if not path.exists():
                print(f"    [OK] Removed via PowerShell")
                return True
        except Exception as e:
            print(f"    [WARNING] PowerShell failed: {e}")
    
    # Method 4: Try to rename and delete later
    if sys.platform == 'win32':
        try:
            backup_name = f"{path.name}_DELETE_{os.getpid()}"
            backup_path = path.parent / backup_name
            path.rename(backup_path)
            print(f"    [INFO] Renamed to {backup_name} (will try to delete later)")
            # Try to delete renamed
            try:
                shutil.rmtree(backup_path, ignore_errors=True)
                if not backup_path.exists():
                    print(f"    [OK] Removed after rename")
                    return True
            except:
                pass
        except Exception as e:
            print(f"    [WARNING] Rename failed: {e}")
    
    return False

def cleanup_backup_directories():
    """Cleanup backup directories from Phoenix folder"""
    base_dir = Path(__file__).parent.parent / 'Phoenix'
    
    print("\n" + "="*70)
    print("Cleaning up backup directories")
    print("="*70)
    
    if not base_dir.exists():
        print(f"[ERROR] Phoenix directory not found: {base_dir}")
        return False
    
    # Find backup directories
    backup_patterns = [
        'phoenix-core-lib-backup*',
        'phoenix-core-lib-empty*'
    ]
    
    backup_dirs = []
    for pattern in backup_patterns:
        # Simple pattern matching
        for item in base_dir.iterdir():
            if item.is_dir():
                if 'phoenix-core-lib-backup' in item.name or 'phoenix-core-lib-empty' in item.name:
                    if item.name != 'phoenix-core-lib':  # Don't remove the main one
                        backup_dirs.append(item)
    
    if not backup_dirs:
        print("\n[INFO] No backup directories found")
        return True
    
    print(f"\nFound {len(backup_dirs)} backup directory(ies):")
    for backup_dir in backup_dirs:
        print(f"  - {backup_dir.name}")
    
    print("\nRemoving backup directories...")
    print("-"*70)
    
    removed_count = 0
    for backup_dir in backup_dirs:
        if remove_directory_robust(backup_dir):
            removed_count += 1
    
    print("\n" + "="*70)
    print(f"Cleanup Summary")
    print("="*70)
    print(f"Total backup directories: {len(backup_dirs)}")
    print(f"Successfully removed: {removed_count}")
    print(f"Failed: {len(backup_dirs) - removed_count}")
    
    # Final check
    remaining = []
    for pattern in backup_patterns:
        for item in base_dir.iterdir():
            if item.is_dir():
                if ('phoenix-core-lib-backup' in item.name or 'phoenix-core-lib-empty' in item.name) and item.name != 'phoenix-core-lib':
                    remaining.append(item.name)
    
    if remaining:
        print(f"\n[WARNING] Some directories could not be removed:")
        for name in remaining:
            print(f"  - {name}")
        print("\nYou may need to remove them manually or restart your computer.")
    else:
        print("\n[OK] All backup directories removed successfully!")
    
    return removed_count == len(backup_dirs)

if __name__ == '__main__':
    try:
        success = cleanup_backup_directories()
        sys.exit(0 if success else 1)
    except Exception as e:
        print(f"\n[ERROR] {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
