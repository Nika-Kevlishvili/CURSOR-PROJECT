"""Fix phoenix-core-lib by cloning fresh with long paths support"""
import os
import sys
import subprocess
import shutil
from pathlib import Path

# Enable long paths in Python (if possible)
if sys.platform == 'win32':
    try:
        # Try to enable long paths via registry (requires admin)
        import winreg
        try:
            key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, 
                                r"SYSTEM\CurrentControlSet\Control\FileSystem", 
                                0, winreg.KEY_WRITE)
            winreg.SetValueEx(key, "LongPathsEnabled", 0, winreg.REG_DWORD, 1)
            winreg.CloseKey(key)
            print("[OK] Enabled Windows long paths support")
        except PermissionError:
            print("[INFO] Could not enable long paths via registry (requires admin)")
        except Exception as e:
            print(f"[WARNING] Could not enable long paths via registry: {e}")
    except ImportError:
        print("[INFO] winreg not available, skipping registry modification")

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
    
    # Method 1: Try shutil.rmtree
    try:
        shutil.rmtree(path)
        if not path.exists():
            return True
    except Exception as e:
        print(f"[WARNING] shutil.rmtree failed: {e}")
    
    # Method 2: Try robocopy (Windows)
    if sys.platform == 'win32':
        try:
            temp_empty = path.parent / f"{path.name}_empty_temp"
            subprocess.run(
                ['robocopy', str(temp_empty), str(path), '/MIR', '/R:0', '/W:0', '/NFL', '/NDL', '/NJH', '/NJS'],
                capture_output=True,
                timeout=30
            )
            if temp_empty.exists():
                shutil.rmtree(temp_empty, ignore_errors=True)
            shutil.rmtree(path, ignore_errors=True)
            if not path.exists():
                return True
        except Exception as e:
            print(f"[WARNING] robocopy method failed: {e}")
    
    # Method 3: Try PowerShell Remove-Item
    if sys.platform == 'win32':
        try:
            subprocess.run(
                ['powershell', '-Command', f'Remove-Item -Path "{path}" -Recurse -Force -ErrorAction SilentlyContinue'],
                timeout=30,
                capture_output=True
            )
            if not path.exists():
                return True
        except Exception as e:
            print(f"[WARNING] PowerShell method failed: {e}")
    
    return False

def clone_phoenix_core_lib():
    """Clone phoenix-core-lib with long paths support"""
    base_dir = Path(__file__).parent.parent / 'Phoenix'
    target_dir = base_dir / 'phoenix-core-lib'
    gitlab_url = os.getenv('GITLAB_URL', 'https://git.domain.internal')
    
    print("\n" + "="*70)
    print("Fixing phoenix-core-lib")
    print("="*70)
    
    # Remove existing directory
    if target_dir.exists():
        print(f"\nRemoving existing directory: {target_dir}")
        if remove_directory_robust(target_dir):
            print("[OK] Directory removed")
        else:
            print("[WARNING] Directory may still exist, but will try to clone anyway")
            # Try to rename it
            try:
                backup_dir = base_dir / f'phoenix-core-lib-backup-{os.getpid()}'
                target_dir.rename(backup_dir)
                print(f"[OK] Renamed to backup: {backup_dir}")
            except Exception as e:
                print(f"[WARNING] Could not rename: {e}")
    
    # Wait a bit
    import time
    time.sleep(2)
    
    # Clone with long paths support
    print(f"\nCloning phoenix-core-lib...")
    print(f"Target: {target_dir}")
    
    env = os.environ.copy()
    env['GIT_SSL_NO_VERIFY'] = '1'
    
    # Configure Git for long paths
    try:
        subprocess.run(['git', 'config', '--global', 'core.longpaths', 'true'], 
                      capture_output=True, timeout=5)
        print("[OK] Enabled Git long paths globally")
    except Exception as e:
        print(f"[WARNING] Could not set global long paths: {e}")
    
    # Clone
    clone_url = f"{gitlab_url}/phoenix/phoenix-core-lib.git"
    print(f"Clone URL: {clone_url}")
    
    try:
        result = subprocess.run(
            ['git', 'clone', '-b', 'main', clone_url, str(target_dir)],
            env=env,
            capture_output=True,
            text=True,
            timeout=600  # 10 minutes
        )
        
        if result.returncode == 0:
            # Set local config
            try:
                subprocess.run(['git', 'config', 'core.longpaths', 'true'],
                             cwd=target_dir, capture_output=True, timeout=5)
            except:
                pass
            
            # Count files
            file_count = sum(1 for _ in target_dir.rglob('*') if _.is_file())
            print(f"\n[SUCCESS] Cloned successfully!")
            print(f"Files: {file_count}")
            
            # Get latest commit
            try:
                commit_result = subprocess.run(
                    ['git', 'log', '-1', '--format=%H|%an|%s'],
                    cwd=target_dir,
                    capture_output=True,
                    text=True,
                    timeout=10
                )
                if commit_result.returncode == 0:
                    parts = commit_result.stdout.strip().split('|')
                    if len(parts) >= 3:
                        print(f"Latest commit: {parts[0][:8]} by {parts[1]}")
                        print(f"Message: {parts[2]}")
            except:
                pass
            
            return True
        else:
            error_msg = result.stderr or result.stdout
            print(f"\n[ERROR] Clone failed:")
            print(error_msg)
            return False
            
    except subprocess.TimeoutExpired:
        print("\n[ERROR] Clone operation timed out")
        return False
    except Exception as e:
        print(f"\n[ERROR] Clone failed: {e}")
        return False

if __name__ == '__main__':
    try:
        success = clone_phoenix_core_lib()
        sys.exit(0 if success else 1)
    except Exception as e:
        print(f"\n[ERROR] {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
