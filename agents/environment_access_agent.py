"""
EnvironmentAccessAgent - Agent for accessing DEV and DEV-2 environments

ROLE:
- Specialized agent for logging into and navigating to DEV and DEV-2 environments
- Handles authentication and environment selection
- Extensible for future environments

CAPABILITIES:
- Login to portal with credentials
- Navigate to DEV environment
- Navigate to DEV-2 environment
- Detect environment buttons by hover color
- Browser automation for environment access
- Study and explore application submenu structure
- Extract menu items, submenus, and navigation hierarchy

BEHAVIOR:
- Receives environment selection requests (DEV, DEV-2)
- Logs into portal automatically
- Navigates to selected environment
- Can explore and study entire submenu structure
- Reports success/failure status
"""

from typing import Dict, Any, Optional, List, TYPE_CHECKING
from enum import Enum
from datetime import datetime
from pathlib import Path
import json
import time

# Playwright imports
try:
    from playwright.sync_api import sync_playwright, Browser, Page, TimeoutError as PlaywrightTimeoutError
    PLAYWRIGHT_AVAILABLE = True
except ImportError:
    PLAYWRIGHT_AVAILABLE = False
    Page = None  # Type placeholder when Playwright is not available
    print("EnvironmentAccessAgent: Playwright not available. Install with: pip install playwright && playwright install")

if TYPE_CHECKING:
    from playwright.sync_api import Page

# Import agent registry
try:
    from .agent_registry import get_agent_registry
    AGENT_REGISTRY_AVAILABLE = True
except ImportError:
    AGENT_REGISTRY_AVAILABLE = False
    print("EnvironmentAccessAgent: Agent registry not available.")


class Environment(Enum):
    """Supported environments."""
    DEV = "dev"
    DEV2 = "dev-2"


class EnvironmentAccessAgent:
    """
    Agent specialized for accessing DEV and DEV-2 environments.
    Handles login and navigation to the selected environment.
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize EnvironmentAccessAgent.
        
        Args:
            config: Configuration dictionary with credentials and URLs
        """
        self.config = config or {}
        
        # Default credentials and URL
        self.login_url = self.config.get(
            'login_url',
            'https://devapps.energo-pro.bg/app/portal/login/?uiLang=en_US'
        )
        self.username = self.config.get('username', 'n10610')
        self.password = self.config.get('password', 'Start#2025')
        
        # Environment access history
        self.access_history: List[Dict[str, Any]] = []
        
        # Browser references for submenu exploration
        self._current_page: Optional['Page'] = None
        self._current_browser: Optional['Browser'] = None
        self._current_context = None
        
        # Initialize agent registry if available
        self.agent_registry = None
        if AGENT_REGISTRY_AVAILABLE:
            try:
                self.agent_registry = get_agent_registry()
                print("EnvironmentAccessAgent: Agent registry available")
            except Exception as e:
                print(f"EnvironmentAccessAgent: Failed to initialize agent registry: {str(e)}")
        
        print("EnvironmentAccessAgent: Initialized")
        print(f"EnvironmentAccessAgent: Login URL: {self.login_url}")
        print("EnvironmentAccessAgent: Ready to access environments")
    
    def access_environment(
        self,
        environment: str,
        use_browser: bool = True
    ) -> Dict[str, Any]:
        """
        Access a specific environment (DEV or DEV-2).
        
        Args:
            environment: Environment name ('dev' or 'dev-2')
            use_browser: Whether to use browser automation (default: True)
        
        Returns:
            Dictionary with access result
        """
        print(f"\n{'='*60}")
        print(f"EnvironmentAccessAgent: Accessing environment: {environment}")
        print(f"{'='*60}\n")
        
        # Validate environment
        env_enum = None
        try:
            if environment.lower() in ['dev', 'dev-1']:
                env_enum = Environment.DEV
            elif environment.lower() in ['dev-2', 'dev2']:
                env_enum = Environment.DEV2
            else:
                return {
                    'success': False,
                    'error': f"Unknown environment: {environment}. Supported: DEV, DEV-2",
                    'timestamp': datetime.now().isoformat()
                }
        except Exception as e:
            return {
                'success': False,
                'error': f"Invalid environment: {str(e)}",
                'timestamp': datetime.now().isoformat()
            }
        
        # Create access record
        access_id = f"ENV_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        access_record = {
            'access_id': access_id,
            'environment': env_enum.value,
            'status': 'running',
            'start_time': datetime.now().isoformat(),
            'method': 'browser' if use_browser else 'api'
        }
        
        try:
            if use_browser:
                result = self._access_via_browser(env_enum)
            else:
                result = {
                    'success': False,
                    'error': 'API access not yet implemented. Use browser access.',
                    'timestamp': datetime.now().isoformat()
                }
            
            # Update access record
            access_record['status'] = 'completed' if result.get('success') else 'failed'
            access_record['end_time'] = datetime.now().isoformat()
            access_record['result'] = result
            
            # Save to history
            self.access_history.append(access_record)
            
            return {
                'success': result.get('success', False),
                'access_id': access_id,
                'environment': env_enum.value,
                'result': result,
                'timestamp': datetime.now().isoformat()
            }
            
        except Exception as e:
            access_record['status'] = 'error'
            access_record['error'] = str(e)
            access_record['end_time'] = datetime.now().isoformat()
            self.access_history.append(access_record)
            
            return {
                'success': False,
                'access_id': access_id,
                'environment': env_enum.value,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    def _access_via_browser(self, environment: Environment) -> Dict[str, Any]:
        """
        Access environment via browser automation using Playwright.
        
        This method uses Playwright to:
        1. Navigate to login page
        2. Enter credentials
        3. Submit login form
        4. Wait for page load
        5. Find "ENERGO-PRO Phoenix" application card
        6. Expand "Other frontends" section
        7. Find and click the appropriate environment button (DEV or DEV-2)
        8. Verify successful navigation
        
        Args:
            environment: Environment enum (DEV or DEV2)
        
        Returns:
            Dictionary with browser access result
        """
        if not PLAYWRIGHT_AVAILABLE:
            return {
                'success': False,
                'error': 'Playwright not available. Install with: pip install playwright && playwright install',
                'timestamp': datetime.now().isoformat()
            }
        
        print("EnvironmentAccessAgent: Starting browser access with Playwright...")
        print(f"EnvironmentAccessAgent: Target environment: {environment.value}")
        
        steps_completed = []
        errors = []
        final_url = None
        
        try:
            with sync_playwright() as p:
                # Launch browser (headless=False to see what's happening)
                print("EnvironmentAccessAgent: Launching browser...")
                browser = p.chromium.launch(headless=False, slow_mo=500)  # slow_mo for visibility
                context = browser.new_context(
                    viewport={'width': 1920, 'height': 1080},
                    user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
                )
                page = context.new_page()
                steps_completed.append('browser_launched')
                
                try:
                    # Step 1: Navigate to login page
                    print("EnvironmentAccessAgent: Step 1 - Navigating to login page...")
                    page.goto(self.login_url, wait_until='networkidle', timeout=30000)
                    steps_completed.append('navigate_to_login')
                    time.sleep(1)
                    
                    # Step 2: Fill username
                    print("EnvironmentAccessAgent: Step 2 - Filling username...")
                    username_input = page.locator('input[type="text"]').first
                    username_input.fill(self.username)
                    steps_completed.append('fill_username')
                    time.sleep(0.5)
                    
                    # Step 3: Fill password
                    print("EnvironmentAccessAgent: Step 3 - Filling password...")
                    password_input = page.locator('input[type="password"]').first
                    password_input.fill(self.password)
                    steps_completed.append('fill_password')
                    time.sleep(0.5)
                    
                    # Step 4: Click login button
                    print("EnvironmentAccessAgent: Step 4 - Clicking login button...")
                    login_button = page.locator('button:has-text("Log in"), button[type="submit"]').first
                    login_button.click()
                    steps_completed.append('click_login')
                    
                    # Step 5: Wait for navigation after login
                    print("EnvironmentAccessAgent: Step 5 - Waiting for page load after login...")
                    page.wait_for_url('**/portal/**', timeout=30000)
                    page.wait_for_load_state('networkidle', timeout=30000)
                    steps_completed.append('wait_for_portal')
                    time.sleep(2)
                    
                    # Step 6: Find "ENERGO-PRO Phoenix" application card
                    print("EnvironmentAccessAgent: Step 6 - Finding ENERGO-PRO Phoenix card...")
                    # Look for card containing "ENERGO-PRO Phoenix" or "Phoenix" text
                    phoenix_card = page.locator('text=ENERGO-PRO Phoenix, text=Phoenix').first
                    if not phoenix_card.is_visible(timeout=10000):
                        # Try alternative: look for card with "Phoenix" in description
                        phoenix_card = page.locator('text=Phoenix').first
                    
                    if phoenix_card.is_visible():
                        steps_completed.append('find_phoenix_card')
                        print("EnvironmentAccessAgent: Found ENERGO-PRO Phoenix card")
                    else:
                        raise Exception("Could not find ENERGO-PRO Phoenix application card")
                    
                    # Step 7: Expand "Other frontends" section
                    print("EnvironmentAccessAgent: Step 7 - Expanding 'Other frontends' section...")
                    # Look for "Other frontends" text or collapsible section
                    other_frontends = page.locator('text=Other frontends, text=/Other frontends/i').first
                    
                    if other_frontends.is_visible(timeout=5000):
                        # Click to expand
                        other_frontends.click()
                        steps_completed.append('expand_other_frontends')
                        print("EnvironmentAccessAgent: Expanded 'Other frontends' section")
                        time.sleep(1)
                    else:
                        # Try to find by parent element and click
                        # Look for element containing "Other frontends" text
                        other_frontends_parent = page.locator('text=/Other frontends/i').locator('..').first
                        if other_frontends_parent.is_visible(timeout=5000):
                            other_frontends_parent.click()
                            steps_completed.append('expand_other_frontends')
                            print("EnvironmentAccessAgent: Expanded 'Other frontends' section (via parent)")
                            time.sleep(1)
                        else:
                            print("EnvironmentAccessAgent: Warning - Could not find 'Other frontends' section, trying to find environment buttons directly")
                    
                    # Step 8: Find and click environment button
                    print(f"EnvironmentAccessAgent: Step 8 - Finding {environment.value.upper()} environment button...")
                    
                    # Look for buttons with environment tags
                    # DEV button should have "dev" tag (not "dev-2")
                    # DEV-2 button should have "dev-2" or "dev2" tag
                    if environment == Environment.DEV:
                        # Look for DEV button - try multiple selectors
                        env_button = (
                            page.locator('text=/ENERGO-PRO Phoenix.*FE.*dev/i').first
                            or page.locator('button:has-text("dev"):not(:has-text("dev-2"))').first
                            or page.locator('[class*="dev"]:not([class*="dev-2"]):has-text("ENERGO-PRO Phoenix")').first
                        )
                    else:  # DEV-2
                        # Look for DEV-2 button
                        env_button = (
                            page.locator('text=/ENERGO-PRO Phoenix.*FE.*dev-2/i').first
                            or page.locator('text=/ENERGO-PRO Phoenix.*FE.*dev2/i').first
                            or page.locator('button:has-text("dev-2"), button:has-text("dev2")').first
                            or page.locator('[class*="dev-2"], [class*="dev2"]:has-text("ENERGO-PRO Phoenix")').first
                        )
                    
                    # Alternative: Look for buttons by hover color (red for DEV-2, different for DEV)
                    # But we'll use text matching first
                    if not env_button.is_visible(timeout=5000):
                        # Try to find all frontend buttons and select by index or text
                        all_frontends = page.locator('text=/ENERGO-PRO Phoenix.*FE/i').all()
                        if len(all_frontends) >= 2:
                            # First one is usually DEV, second is DEV-2
                            if environment == Environment.DEV:
                                env_button = all_frontends[0]
                            else:
                                env_button = all_frontends[1] if len(all_frontends) > 1 else all_frontends[0]
                    
                    if env_button.is_visible(timeout=10000):
                        print(f"EnvironmentAccessAgent: Found {environment.value.upper()} button, clicking...")
                        env_button.click()
                        steps_completed.append('click_environment_button')
                        time.sleep(2)
                        
                        # Step 9: Wait for navigation
                        print("EnvironmentAccessAgent: Step 9 - Waiting for navigation...")
                        page.wait_for_load_state('networkidle', timeout=30000)
                        final_url = page.url
                        steps_completed.append('wait_for_navigation')
                        
                        print(f"EnvironmentAccessAgent: Successfully navigated to: {final_url}")
                    else:
                        raise Exception(f"Could not find {environment.value.upper()} environment button")
                    
                    # Keep browser open for a moment to verify
                    time.sleep(2)
                    
                    # Store page reference for submenu exploration
                    self._current_page = page
                    self._current_browser = browser
                    self._current_context = context
                    
                    return {
                        'success': True,
                        'method': 'playwright',
                        'environment': environment.value,
                        'final_url': final_url,
                        'steps_completed': steps_completed,
                        'message': f'Successfully accessed {environment.value.upper()} environment',
                        'page_available': True,
                        'timestamp': datetime.now().isoformat()
                    }
                    
                except PlaywrightTimeoutError as e:
                    errors.append(f"Timeout error: {str(e)}")
                    return {
                        'success': False,
                        'method': 'playwright',
                        'environment': environment.value,
                        'steps_completed': steps_completed,
                        'errors': errors,
                        'error': f'Timeout: {str(e)}',
                        'timestamp': datetime.now().isoformat()
                    }
                except Exception as e:
                    errors.append(str(e))
                    return {
                        'success': False,
                        'method': 'playwright',
                        'environment': environment.value,
                        'steps_completed': steps_completed,
                        'errors': errors,
                        'error': str(e),
                        'timestamp': datetime.now().isoformat()
                    }
                finally:
                    # Keep browser open - do NOT close automatically
                    # Browser will remain open for user interaction
                    print("\n" + "="*60)
                    print("EnvironmentAccessAgent: Browser will remain open.")
                    print("EnvironmentAccessAgent: Please close it manually when done.")
                    print("="*60 + "\n")
                    # Do NOT call browser.close() - keep browser open
                    pass
                    
        except Exception as e:
            errors.append(str(e))
            return {
                'success': False,
                'method': 'playwright',
                'environment': environment.value,
                'steps_completed': steps_completed,
                'errors': errors,
                'error': f'Browser automation failed: {str(e)}',
                'timestamp': datetime.now().isoformat()
            }
    
    def get_access_history(self) -> List[Dict[str, Any]]:
        """Get history of environment access attempts."""
        return self.access_history.copy()
    
    def get_last_access(self) -> Optional[Dict[str, Any]]:
        """Get last environment access record."""
        return self.access_history[-1] if self.access_history else None
    
    def study_submenu(self, page: Optional['Page'] = None) -> Dict[str, Any]:
        """
        Study and explore the entire submenu structure of the application.
        
        This method:
        1. Finds the main menu/navigation element
        2. Identifies all menu items
        3. Expands all expandable submenus
        4. Extracts menu structure (text, links, hierarchy)
        5. Returns comprehensive menu structure data
        
        Args:
            page: Optional Playwright Page object. If not provided, uses stored page from access_environment.
        
        Returns:
            Dictionary with menu structure information
        """
        if not PLAYWRIGHT_AVAILABLE:
            return {
                'success': False,
                'error': 'Playwright not available. Install with: pip install playwright && playwright install',
                'timestamp': datetime.now().isoformat()
            }
        
        # Use provided page or stored page from access_environment
        target_page = page or self._current_page
        if not target_page:
            return {
                'success': False,
                'error': 'No page available. Please access environment first using access_environment()',
                'timestamp': datetime.now().isoformat()
            }
        
        print(f"\n{'='*60}")
        print("EnvironmentAccessAgent: Starting submenu exploration...")
        print(f"{'='*60}\n")
        
        menu_structure = {
            'main_menu_items': [],
            'submenus': {},
            'all_items': [],
            'hierarchy': {},
            'total_items': 0,
            'exploration_steps': []
        }
        
        try:
            # Wait for page to be fully loaded
            print("EnvironmentAccessAgent: Waiting for page to be fully loaded...")
            target_page.wait_for_load_state('networkidle', timeout=10000)
            time.sleep(2)
            menu_structure['exploration_steps'].append('page_loaded')
            
            # Step 1: Find main navigation/menu element
            print("EnvironmentAccessAgent: Step 1 - Finding main navigation menu...")
            menu_selectors = [
                'nav',
                '[role="navigation"]',
                '.menu',
                '.sidebar',
                '.navigation',
                '[class*="menu"]',
                '[class*="nav"]',
                '[class*="sidebar"]',
                'aside',
                '[class*="drawer"]'
            ]
            
            main_menu = None
            for selector in menu_selectors:
                try:
                    locator = target_page.locator(selector).first
                    if locator.is_visible(timeout=2000):
                        main_menu = locator
                        print(f"EnvironmentAccessAgent: Found menu using selector: {selector}")
                        menu_structure['exploration_steps'].append(f'menu_found_{selector}')
                        break
                except:
                    continue
            
            if not main_menu:
                # Try to find menu items directly
                print("EnvironmentAccessAgent: Trying to find menu items directly...")
                menu_items = target_page.locator('a[href], button, [role="menuitem"], [class*="menu-item"]').all()
                if menu_items:
                    print(f"EnvironmentAccessAgent: Found {len(menu_items)} potential menu items")
                    menu_structure['exploration_steps'].append('menu_items_found_directly')
            
            # Step 2: Find all menu items and links
            print("EnvironmentAccessAgent: Step 2 - Extracting menu items...")
            
            # Common menu item selectors
            item_selectors = [
                'a[href]',
                'button',
                '[role="menuitem"]',
                '[role="button"]',
                '[class*="menu-item"]',
                '[class*="nav-item"]',
                '[class*="list-item"]',
                'li > a',
                '.MuiListItem-root',
                '.ant-menu-item'
            ]
            
            all_menu_items = []
            seen_texts = set()
            
            for selector in item_selectors:
                try:
                    items = target_page.locator(selector).all()
                    for item in items:
                        try:
                            if not item.is_visible(timeout=1000):
                                continue
                            
                            text = item.inner_text(timeout=1000).strip()
                            if not text or text in seen_texts:
                                continue
                            
                            href = None
                            try:
                                href = item.get_attribute('href')
                            except:
                                pass
                            
                            # Get element info
                            tag_name = item.evaluate('el => el.tagName.toLowerCase()')
                            classes = item.get_attribute('class') or ''
                            
                            item_info = {
                                'text': text,
                                'href': href,
                                'tag': tag_name,
                                'classes': classes,
                                'selector': selector,
                                'has_submenu': False,
                                'submenu_items': []
                            }
                            
                            # Check if item is expandable (has submenu)
                            try:
                                # Look for expand indicators (arrows, chevrons, etc.)
                                parent = item.locator('..')
                                has_arrow = (
                                    parent.locator('[class*="arrow"], [class*="chevron"], [class*="expand"]').count() > 0
                                    or item.locator('[class*="arrow"], [class*="chevron"], [class*="expand"]').count() > 0
                                )
                                
                                # Check for aria-expanded
                                aria_expanded = item.get_attribute('aria-expanded')
                                if aria_expanded == 'false' or has_arrow:
                                    item_info['has_submenu'] = True
                                    item_info['expandable'] = True
                            except:
                                pass
                            
                            all_menu_items.append(item_info)
                            seen_texts.add(text)
                        except:
                            continue
                except:
                    continue
            
            # Remove duplicates and filter meaningful items
            unique_items = []
            for item in all_menu_items:
                if item['text'] and len(item['text']) > 0:
                    # Skip very short or common UI elements
                    if len(item['text']) < 2:
                        continue
                    unique_items.append(item)
            
            menu_structure['all_items'] = unique_items
            menu_structure['total_items'] = len(unique_items)
            print(f"EnvironmentAccessAgent: Found {len(unique_items)} menu items")
            menu_structure['exploration_steps'].append(f'extracted_{len(unique_items)}_items')
            
            # Step 3: Try to expand ALL submenus and explore complete hierarchy
            print("EnvironmentAccessAgent: Step 3 - Exploring and expanding ALL submenu hierarchy...")
            print("EnvironmentAccessAgent: This may take a while as we explore all menu items...")
            
            expanded_count = 0
            expanded_items = set()  # Track which items we've already expanded
            
            # First, find all expandable menu items
            expandable_items = [item for item in unique_items if item.get('expandable') or item.get('has_submenu')]
            print(f"EnvironmentAccessAgent: Found {len(expandable_items)} potentially expandable items")
            
            # Try to expand each expandable item
            for idx, item_info in enumerate(expandable_items, 1):
                try:
                    item_text = item_info['text']
                    if item_text in expanded_items:
                        continue  # Already expanded
                    
                    print(f"EnvironmentAccessAgent: Expanding item {idx}/{len(expandable_items)}: {item_text[:50]}...")
                    
                    # Try multiple ways to find and click the menu item
                    item_locator = None
                    
                    # Method 1: Direct text match
                    try:
                        item_locator = target_page.locator(f'text="{item_text}"').first
                        if not item_locator.is_visible(timeout=1000):
                            item_locator = None
                    except:
                        pass
                    
                    # Method 2: Partial text match
                    if not item_locator:
                        try:
                            # Try to find by partial text (first 20 chars)
                            partial_text = item_text[:20].strip()
                            if partial_text:
                                item_locator = target_page.locator(f'text=/^{partial_text}/i').first
                                if not item_locator.is_visible(timeout=1000):
                                    item_locator = None
                        except:
                            pass
                    
                    # Method 3: Find by classes and tag
                    if not item_locator:
                        try:
                            classes = item_info.get('classes', '')
                            tag = item_info.get('tag', '')
                            if classes:
                                # Try to find element with matching classes
                                item_locator = target_page.locator(f'{tag}.{classes.split()[0]}').first
                                if not item_locator.is_visible(timeout=1000):
                                    item_locator = None
                        except:
                            pass
                    
                    if item_locator and item_locator.is_visible(timeout=2000):
                        # Scroll into view
                        item_locator.scroll_into_view_if_needed()
                        time.sleep(0.5)
                        
                        # Check if already expanded
                        try:
                            aria_expanded = item_locator.get_attribute('aria-expanded')
                            if aria_expanded == 'true':
                                print(f"  Already expanded: {item_text[:50]}")
                                expanded_items.add(item_text)
                                continue
                        except:
                            pass
                        
                        # Click to expand
                        try:
                            item_locator.click()
                            time.sleep(1.5)  # Wait for submenu to appear
                            expanded_items.add(item_text)
                            
                            # Now look for submenu items
                            submenu_items_found = []
                            
                            # Method 1: Look for items in the same parent container
                            try:
                                parent = item_locator.locator('..')
                                submenu_items = parent.locator('a, button, [role="menuitem"], [class*="menu-item"], [class*="submenu"]').all()
                                
                                for sub_item in submenu_items:
                                    try:
                                        sub_text = sub_item.inner_text(timeout=500).strip()
                                        if sub_text and sub_text != item_text and len(sub_text) > 0:
                                            sub_href = None
                                            try:
                                                sub_href = sub_item.get_attribute('href')
                                            except:
                                                pass
                                            submenu_items_found.append({
                                                'text': sub_text,
                                                'href': sub_href
                                            })
                                    except:
                                        continue
                            except:
                                pass
                            
                            # Method 2: Look for items with common submenu selectors
                            if not submenu_items_found:
                                try:
                                    # Common submenu patterns
                                    submenu_selectors = [
                                        '[class*="submenu"] a',
                                        '[class*="sub-menu"] a',
                                        '.ant-menu-submenu a',
                                        '.MuiMenuItem-root',
                                        '[role="menuitem"]'
                                    ]
                                    
                                    for selector in submenu_selectors:
                                        try:
                                            sub_items = target_page.locator(selector).all()
                                            for sub_item in sub_items:
                                                try:
                                                    sub_text = sub_item.inner_text(timeout=500).strip()
                                                    if sub_text and sub_text != item_text and len(sub_text) > 0:
                                                        sub_href = None
                                                        try:
                                                            sub_href = sub_item.get_attribute('href')
                                                        except:
                                                            pass
                                                        if not any(s['text'] == sub_text for s in submenu_items_found):
                                                            submenu_items_found.append({
                                                                'text': sub_text,
                                                                'href': sub_href
                                                            })
                                                except:
                                                    continue
                                        except:
                                            continue
                                except:
                                    pass
                            
                            if submenu_items_found:
                                item_info['submenu_items'] = submenu_items_found
                                item_info['has_submenu'] = True
                                item_info['expanded'] = True
                                expanded_count += 1
                                print(f"  ✓ Found {len(submenu_items_found)} submenu items")
                            else:
                                print(f"  - No submenu items found")
                                
                        except Exception as e:
                            print(f"  ✗ Error clicking: {str(e)[:50]}")
                            continue
                    else:
                        print(f"  - Item not visible or not found")
                        
                except Exception as e:
                    print(f"  ✗ Error processing item: {str(e)[:50]}")
                    continue
            
            print(f"\nEnvironmentAccessAgent: Successfully expanded {expanded_count} submenus")
            menu_structure['exploration_steps'].append(f'expanded_{expanded_count}_submenus')
            
            # Step 4: Group items by category (if possible)
            print("EnvironmentAccessAgent: Step 4 - Organizing menu structure...")
            
            # Try to identify main menu sections
            main_sections = []
            current_section = None
            
            for item in unique_items:
                # Check if item looks like a section header
                is_section = (
                    item.get('tag') == 'div' or
                    'header' in item.get('classes', '').lower() or
                    'section' in item.get('classes', '').lower() or
                    not item.get('href')  # Section headers often don't have links
                )
                
                if is_section and len(item['text']) > 0:
                    if current_section:
                        main_sections.append(current_section)
                    current_section = {
                        'name': item['text'],
                        'items': []
                    }
                elif current_section:
                    current_section['items'].append(item)
                else:
                    main_sections.append({
                        'name': 'Main Menu',
                        'items': [item]
                    })
                    current_section = None
            
            if current_section:
                main_sections.append(current_section)
            
            menu_structure['main_menu_items'] = main_sections if main_sections else [{'name': 'All Items', 'items': unique_items}]
            
            # Step 5: Extract page structure (titles, headings)
            print("EnvironmentAccessAgent: Step 5 - Extracting page structure...")
            try:
                page_title = target_page.title()
                headings = []
                for i in range(1, 7):
                    h_elements = target_page.locator(f'h{i}').all()
                    for h in h_elements:
                        try:
                            if h.is_visible(timeout=500):
                                headings.append({
                                    'level': i,
                                    'text': h.inner_text(timeout=500).strip()
                                })
                        except:
                            continue
                
                menu_structure['page_info'] = {
                    'title': page_title,
                    'headings': headings,
                    'url': target_page.url
                }
            except:
                pass
            
            print(f"\n{'='*60}")
            print("EnvironmentAccessAgent: Submenu exploration completed!")
            print(f"Total items found: {menu_structure['total_items']}")
            print(f"Submenus expanded: {expanded_count}")
            print(f"{'='*60}\n")
            
            # Step 6: Save menu structure to file
            print("EnvironmentAccessAgent: Step 6 - Saving menu structure to file...")
            saved_file = self._save_menu_structure(menu_structure, target_page.url)
            if saved_file:
                print(f"EnvironmentAccessAgent: Menu structure saved to: {saved_file}")
                menu_structure['saved_to_file'] = str(saved_file)
            
            return {
                'success': True,
                'menu_structure': menu_structure,
                'saved_file': str(saved_file) if saved_file else None,
                'timestamp': datetime.now().isoformat()
            }
            
        except Exception as e:
            import traceback
            error_trace = traceback.format_exc()
            print(f"EnvironmentAccessAgent: Error during submenu exploration: {str(e)}")
            print(f"Traceback: {error_trace}")
            
            return {
                'success': False,
                'error': str(e),
                'error_trace': error_trace,
                'partial_structure': menu_structure,
                'timestamp': datetime.now().isoformat()
            }
    
    def access_and_study_submenu(
        self,
        environment: str,
        use_browser: bool = True
    ) -> Dict[str, Any]:
        """
        Access environment and automatically study the submenu.
        
        This is a convenience method that combines access_environment and study_submenu.
        It keeps the browser context open to allow submenu exploration.
        
        Args:
            environment: Environment name ('dev' or 'dev-2')
            use_browser: Whether to use browser automation (default: True)
        
        Returns:
            Dictionary with access and submenu study results
        """
        print(f"\n{'='*60}")
        print(f"EnvironmentAccessAgent: Accessing {environment.upper()} and studying submenu...")
        print(f"{'='*60}\n")
        
        if not PLAYWRIGHT_AVAILABLE:
            return {
                'success': False,
                'error': 'Playwright not available. Install with: pip install playwright && playwright install',
                'timestamp': datetime.now().isoformat()
            }
        
        # Validate environment
        env_enum = None
        try:
            if environment.lower() in ['dev', 'dev-1']:
                env_enum = Environment.DEV
            elif environment.lower() in ['dev-2', 'dev2']:
                env_enum = Environment.DEV2
            else:
                return {
                    'success': False,
                    'error': f"Unknown environment: {environment}. Supported: DEV, DEV-2",
                    'timestamp': datetime.now().isoformat()
                }
        except Exception as e:
            return {
                'success': False,
                'error': f"Invalid environment: {str(e)}",
                'timestamp': datetime.now().isoformat()
            }
        
        # Access environment and study submenu in the same browser session
        # Note: We use start() instead of 'with' to keep browser open
        playwright_instance = None
        browser = None
        try:
            # Start playwright without context manager to keep it alive
            playwright_instance = sync_playwright().start()
            # Launch browser with detach=True so it stays open even if Python process ends
            print("EnvironmentAccessAgent: Launching browser...")
            browser = playwright_instance.chromium.launch(
                headless=False, 
                slow_mo=500,
                args=['--disable-blink-features=AutomationControlled']  # Make it look more like normal browser
            )
            context = browser.new_context(
                viewport={'width': 1920, 'height': 1080},
                user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            )
            page = context.new_page()
            
            try:
                # Access environment (same steps as _access_via_browser)
                access_result = self._access_environment_in_context(page, env_enum)
                
                if not access_result.get('success'):
                    return {
                        'success': False,
                        'access_result': access_result,
                        'error': 'Failed to access environment',
                        'timestamp': datetime.now().isoformat()
                    }
                
                # Step 10: Navigate to Phoenix application from portal
                print("EnvironmentAccessAgent: Step 10 - Navigating to Phoenix application...")
                phoenix_app_result = self._navigate_to_phoenix_app(page, env_enum)
                
                if not phoenix_app_result.get('success'):
                    print(f"EnvironmentAccessAgent: Warning - Could not navigate to Phoenix app: {phoenix_app_result.get('error')}")
                    # Continue anyway, maybe we're already in Phoenix
                else:
                    print(f"EnvironmentAccessAgent: Successfully navigated to Phoenix application")
                
                # Wait for page to stabilize
                time.sleep(2)
                
                # Keep browser open - wait for user input before closing
                print("\n" + "="*60)
                print("EnvironmentAccessAgent: Browser will remain open.")
                print("EnvironmentAccessAgent: Press Enter in the terminal to close the browser...")
                print("="*60 + "\n")
                
                result = {
                    'success': True,
                    'access_result': access_result,
                    'timestamp': datetime.now().isoformat()
                }
                
                # Keep browser open - NEVER close automatically
                # Browser will remain open until user manually closes it
                print("\n" + "="*60)
                print("EnvironmentAccessAgent: Browser will remain open.")
                print("EnvironmentAccessAgent: Please close it manually when you are done.")
                print("EnvironmentAccessAgent: Script will keep running to maintain browser connection.")
                print("EnvironmentAccessAgent: Press Ctrl+C to stop script (browser will stay open).")
                print("="*60 + "\n")
                
                # Keep script running to maintain browser connection
                # Browser will NOT close automatically - user must close it manually
                try:
                    while True:
                        time.sleep(1)
                        # Check if browser is still connected
                        try:
                            if browser and browser.is_connected():
                                # Browser still open - continue
                                pass
                            else:
                                # Browser was closed by user
                                print("EnvironmentAccessAgent: Browser was closed by user.")
                                break
                        except:
                            # Browser might be closed
                            break
                except KeyboardInterrupt:
                    # User pressed Ctrl+C - keep browser open anyway
                    print("\nEnvironmentAccessAgent: Script stopped (Ctrl+C).")
                    print("EnvironmentAccessAgent: Browser will remain open - please close it manually.")
                    # Do NOT close browser or playwright - keep them open
                except Exception:
                    # Any other exception - keep browser open
                    print("\nEnvironmentAccessAgent: Script encountered an issue.")
                    print("EnvironmentAccessAgent: Browser will remain open - please close it manually.")
                    # Do NOT close browser or playwright - keep them open
                
                # Do NOT close browser or playwright here - user must close manually
                
                return result
                
            except Exception as e:
                # Do NOT close browser on error - keep it open for user
                print(f"\nEnvironmentAccessAgent: Error occurred: {str(e)}")
                print("EnvironmentAccessAgent: Browser will remain open for inspection.")
                return {
                    'success': False,
                    'error': f'Error during access/exploration: {str(e)}',
                    'timestamp': datetime.now().isoformat()
                }
                    
        except Exception as e:
            # Do NOT close browser on exception - keep it open for user
            print(f"\nEnvironmentAccessAgent: Exception occurred: {str(e)}")
            print("EnvironmentAccessAgent: Browser will remain open for inspection.")
            return {
                'success': False,
                'error': f'Browser automation failed: {str(e)}',
                'timestamp': datetime.now().isoformat()
            }
    
    def _access_environment_in_context(self, page: 'Page', environment: Environment) -> Dict[str, Any]:
        """
        Access environment using an existing page context.
        Internal method used by access_and_study_submenu.
        """
        steps_completed = []
        final_url = None
        
        try:
            # Step 1: Navigate to login page
            print("EnvironmentAccessAgent: Step 1 - Navigating to login page...")
            page.goto(self.login_url, wait_until='networkidle', timeout=30000)
            steps_completed.append('navigate_to_login')
            time.sleep(1)
            
            # Step 2: Fill username
            print("EnvironmentAccessAgent: Step 2 - Filling username...")
            username_input = page.locator('input[type="text"]').first
            username_input.fill(self.username)
            steps_completed.append('fill_username')
            time.sleep(0.5)
            
            # Step 3: Fill password
            print("EnvironmentAccessAgent: Step 3 - Filling password...")
            password_input = page.locator('input[type="password"]').first
            password_input.fill(self.password)
            steps_completed.append('fill_password')
            time.sleep(0.5)
            
            # Step 4: Click login button
            print("EnvironmentAccessAgent: Step 4 - Clicking login button...")
            login_button = page.locator('button:has-text("Log in"), button[type="submit"]').first
            login_button.click()
            steps_completed.append('click_login')
            
            # Step 5: Wait for navigation after login
            print("EnvironmentAccessAgent: Step 5 - Waiting for page load after login...")
            page.wait_for_url('**/portal/**', timeout=30000)
            page.wait_for_load_state('networkidle', timeout=30000)
            steps_completed.append('wait_for_portal')
            time.sleep(2)
            
            # Step 6: Find "ENERGO-PRO Phoenix" application card
            print("EnvironmentAccessAgent: Step 6 - Finding ENERGO-PRO Phoenix card...")
            phoenix_card = page.locator('text=ENERGO-PRO Phoenix, text=Phoenix').first
            if not phoenix_card.is_visible(timeout=10000):
                phoenix_card = page.locator('text=Phoenix').first
            
            if phoenix_card.is_visible():
                steps_completed.append('find_phoenix_card')
                print("EnvironmentAccessAgent: Found ENERGO-PRO Phoenix card")
            else:
                raise Exception("Could not find ENERGO-PRO Phoenix application card")
            
            # Step 7: Expand "Other frontends" section
            print("EnvironmentAccessAgent: Step 7 - Expanding 'Other frontends' section...")
            other_frontends = page.locator('text=Other frontends, text=/Other frontends/i').first
            
            if other_frontends.is_visible(timeout=5000):
                other_frontends.click()
                steps_completed.append('expand_other_frontends')
                print("EnvironmentAccessAgent: Expanded 'Other frontends' section")
                time.sleep(1)
            else:
                other_frontends_parent = page.locator('text=/Other frontends/i').locator('..').first
                if other_frontends_parent.is_visible(timeout=5000):
                    other_frontends_parent.click()
                    steps_completed.append('expand_other_frontends')
                    print("EnvironmentAccessAgent: Expanded 'Other frontends' section (via parent)")
                    time.sleep(1)
            
            # Step 8: Find and click environment button
            print(f"EnvironmentAccessAgent: Step 8 - Finding {environment.value.upper()} environment button...")
            
            if environment == Environment.DEV:
                env_button = (
                    page.locator('text=/ENERGO-PRO Phoenix.*FE.*dev/i').first
                    or page.locator('button:has-text("dev"):not(:has-text("dev-2"))').first
                    or page.locator('[class*="dev"]:not([class*="dev-2"]):has-text("ENERGO-PRO Phoenix")').first
                )
            else:  # DEV-2
                env_button = (
                    page.locator('text=/ENERGO-PRO Phoenix.*FE.*dev-2/i').first
                    or page.locator('text=/ENERGO-PRO Phoenix.*FE.*dev2/i').first
                    or page.locator('button:has-text("dev-2"), button:has-text("dev2")').first
                    or page.locator('[class*="dev-2"], [class*="dev2"]:has-text("ENERGO-PRO Phoenix")').first
                )
            
            if not env_button.is_visible(timeout=5000):
                all_frontends = page.locator('text=/ENERGO-PRO Phoenix.*FE/i').all()
                if len(all_frontends) >= 2:
                    if environment == Environment.DEV:
                        env_button = all_frontends[0]
                    else:
                        env_button = all_frontends[1] if len(all_frontends) > 1 else all_frontends[0]
            
            if env_button.is_visible(timeout=10000):
                print(f"EnvironmentAccessAgent: Found {environment.value.upper()} button, clicking...")
                env_button.click()
                steps_completed.append('click_environment_button')
                time.sleep(2)
                
                # Step 9: Wait for navigation
                print("EnvironmentAccessAgent: Step 9 - Waiting for navigation...")
                page.wait_for_load_state('networkidle', timeout=30000)
                final_url = page.url
                steps_completed.append('wait_for_navigation')
                
                print(f"EnvironmentAccessAgent: Successfully navigated to: {final_url}")
            else:
                raise Exception(f"Could not find {environment.value.upper()} environment button")
            
            time.sleep(2)
            
            return {
                'success': True,
                'method': 'playwright',
                'environment': environment.value,
                'final_url': final_url,
                'steps_completed': steps_completed,
                'message': f'Successfully accessed {environment.value.upper()} environment',
                'timestamp': datetime.now().isoformat()
            }
            
        except Exception as e:
            return {
                'success': False,
                'method': 'playwright',
                'environment': environment.value,
                'steps_completed': steps_completed,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    def _navigate_to_phoenix_app(self, page: 'Page', environment: Environment) -> Dict[str, Any]:
        """
        Navigate from portal to Phoenix application.
        Internal method used by access_and_study_submenu.
        """
        try:
            # Wait for portal page to load
            page.wait_for_load_state('networkidle', timeout=10000)
            time.sleep(2)
            
            # Look for Phoenix application button
            # For DEV: "ENERGO-PRO Phoenix-1 FE DEV" or similar
            # For DEV-2: "ENERGO-PRO Phoenix-1 FE DEV-2" or similar
            
            phoenix_button_selectors = [
                f'button:has-text("ENERGO-PRO Phoenix"):has-text("{environment.value.upper()}")',
                f'button:has-text("Phoenix"):has-text("{environment.value.upper()}")',
                f'button:has-text("ENERGO-PRO Phoenix-1 FE {environment.value.upper()}")',
                f'button.frontendButton:has-text("Phoenix")',
                'button.frontendButton:has-text("ENERGO-PRO Phoenix-1 FE DEV")' if environment == Environment.DEV else 'button.frontendButton:has-text("ENERGO-PRO Phoenix-1 FE DEV-2")'
            ]
            
            phoenix_button = None
            for selector in phoenix_button_selectors:
                try:
                    button = page.locator(selector).first
                    if button.is_visible(timeout=3000):
                        phoenix_button = button
                        print(f"EnvironmentAccessAgent: Found Phoenix button with selector: {selector}")
                        break
                except:
                    continue
            
            # If not found, try to find all frontend buttons and select Phoenix
            if not phoenix_button:
                all_buttons = page.locator('button.frontendButton').all()
                for btn in all_buttons:
                    try:
                        text = btn.inner_text(timeout=1000)
                        if 'Phoenix' in text and environment.value.upper() in text:
                            phoenix_button = btn
                            print(f"EnvironmentAccessAgent: Found Phoenix button by text: {text}")
                            break
                    except:
                        continue
            
            if phoenix_button and phoenix_button.is_visible(timeout=5000):
                print("EnvironmentAccessAgent: Clicking Phoenix application button...")
                
                # Get page count before clicking
                pages_before = len(page.context.pages)
                
                # Click Phoenix button
                phoenix_button.click()
                
                # Wait for navigation
                time.sleep(3)
                
                # Check if new tab was opened
                pages_after = len(page.context.pages)
                
                if pages_after > pages_before:
                    # New tab was opened - close it, stay on same page
                    print("EnvironmentAccessAgent: New tab opened, closing it...")
                    new_page = page.context.pages[-1]
                    new_page.close()
                    # Wait for same page to navigate (if it does)
                    page.wait_for_load_state('networkidle', timeout=30000)
                    time.sleep(2)
                    return {
                        'success': True,
                        'message': 'Navigated to Phoenix application (closed new tab)',
                        'url': page.url
                    }
                else:
                    # Same page navigation - just wait
                    page.wait_for_load_state('networkidle', timeout=30000)
                    time.sleep(2)
                    return {
                        'success': True,
                        'message': 'Navigated to Phoenix application',
                        'url': page.url
                    }
            else:
                # Maybe we're already in Phoenix app
                current_url = page.url
                if 'phoenix' in current_url.lower() or 'app' in current_url.lower():
                    return {
                        'success': True,
                        'message': 'Already in Phoenix application',
                        'url': current_url
                    }
                else:
                    return {
                        'success': False,
                        'error': 'Could not find Phoenix application button',
                        'url': current_url
                    }
                    
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'url': page.url if page else None
            }
    
    def _save_menu_structure(self, menu_structure: Dict[str, Any], url: str) -> Optional[Path]:
        """
        Save menu structure to JSON file.
        
        Args:
            menu_structure: Menu structure dictionary
            url: Current page URL
        
        Returns:
            Path to saved file or None if failed
        """
        try:
            # Create menu_data directory if it doesn't exist
            menu_data_dir = Path(__file__).parent.parent / "menu_data"
            menu_data_dir.mkdir(exist_ok=True)
            
            # Generate filename with timestamp
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"phoenix_menu_structure_{timestamp}.json"
            file_path = menu_data_dir / filename
            
            # Prepare data to save
            data_to_save = {
                'metadata': {
                    'export_date': datetime.now().isoformat(),
                    'url': url,
                    'total_items': menu_structure.get('total_items', 0),
                    'total_submenus': sum(1 for item in menu_structure.get('all_items', []) if item.get('has_submenu'))
                },
                'menu_structure': menu_structure
            }
            
            # Save to file
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data_to_save, f, indent=2, ensure_ascii=False)
            
            return file_path
            
        except Exception as e:
            print(f"EnvironmentAccessAgent: Error saving menu structure: {str(e)}")
            return None
    
    def navigate_to_customer_listing(self, page: Optional['Page'] = None) -> Dict[str, Any]:
        """
        Navigate to Customer Listing page in Phoenix application.
        
        This method:
        1. Finds Customer menu item
        2. Expands it if needed
        3. Clicks on Customer Listing
        4. Waits for page to load
        
        Args:
            page: Optional Playwright Page object. If not provided, uses stored page.
        
        Returns:
            Dictionary with navigation result
        """
        if not PLAYWRIGHT_AVAILABLE:
            return {
                'success': False,
                'error': 'Playwright not available',
                'timestamp': datetime.now().isoformat()
            }
        
        target_page = page or self._current_page
        if not target_page:
            return {
                'success': False,
                'error': 'No page available. Please access environment first.',
                'timestamp': datetime.now().isoformat()
            }
        
        print("\n" + "="*60)
        print("EnvironmentAccessAgent: Navigating to Customer Listing...")
        print("="*60 + "\n")
        
        try:
            # Wait for page to be ready
            target_page.wait_for_load_state('networkidle', timeout=10000)
            time.sleep(1)
            
            # Step 0: Open hamburger menu
            print("EnvironmentAccessAgent: Step 0 - Opening hamburger menu...")
            hamburger_menu = None
            
            # Try to find burger menu by class
            try:
                hamburger_menu = target_page.locator('div.burger, div[class*="burger"]').first
                if hamburger_menu.is_visible(timeout=2000):
                    print("EnvironmentAccessAgent: Found hamburger menu by class")
                else:
                    hamburger_menu = None
            except:
                pass
            
            # If not found, try by tabindex
            if not hamburger_menu:
                try:
                    hamburger_menu = target_page.locator('div[tabindex="1"]').first
                    if hamburger_menu.is_visible(timeout=2000):
                        print("EnvironmentAccessAgent: Found hamburger menu by tabindex")
                    else:
                        hamburger_menu = None
                except:
                    pass
            
            # If still not found, try to find any div with burger in class
            if not hamburger_menu:
                try:
                    all_divs = target_page.locator('div[class*="burger"]').all()
                    for div in all_divs:
                        if div.is_visible(timeout=500):
                            hamburger_menu = div
                            print("EnvironmentAccessAgent: Found hamburger menu in burger divs")
                            break
                except:
                    pass
            
            if hamburger_menu:
                print("EnvironmentAccessAgent: Clicking hamburger menu...")
                hamburger_menu.click()
                time.sleep(2)  # Wait for menu to open
                print("EnvironmentAccessAgent: Hamburger menu opened")
            else:
                print("EnvironmentAccessAgent: Could not find hamburger menu, trying to continue...")
            
            # Step 1: Find Customer menu item
            print("EnvironmentAccessAgent: Step 1 - Finding Customer menu item...")
            
            customer_menu_selectors = [
                'text=/Customer/i',
                'text=Customer',
                '[class*="menu"]:has-text("Customer")',
                'a:has-text("Customer")',
                'button:has-text("Customer")',
                '[role="menuitem"]:has-text("Customer")'
            ]
            
            customer_menu = None
            for selector in customer_menu_selectors:
                try:
                    locator = target_page.locator(selector).first
                    if locator.is_visible(timeout=2000):
                        customer_menu = locator
                        print(f"EnvironmentAccessAgent: Found Customer menu with selector: {selector}")
                        break
                except:
                    continue
            
            if not customer_menu:
                # Try to find all menu items and search for Customer
                print("EnvironmentAccessAgent: Trying alternative method - searching all menu items...")
                all_menu_items = target_page.locator('a, button, [role="menuitem"]').all()
                for item in all_menu_items:
                    try:
                        text = item.inner_text(timeout=500).strip()
                        if 'customer' in text.lower() and 'listing' not in text.lower():
                            customer_menu = item
                            print(f"EnvironmentAccessAgent: Found Customer menu item: {text}")
                            break
                    except:
                        continue
            
            if not customer_menu:
                return {
                    'success': False,
                    'error': 'Could not find Customer menu item',
                    'timestamp': datetime.now().isoformat()
                }
            
            # Step 2: Check if Customer menu needs to be expanded
            print("EnvironmentAccessAgent: Step 2 - Checking if Customer menu needs expansion...")
            try:
                customer_menu.scroll_into_view_if_needed()
                time.sleep(0.5)
                
                # Check aria-expanded
                aria_expanded = customer_menu.get_attribute('aria-expanded')
                if aria_expanded == 'false':
                    print("EnvironmentAccessAgent: Customer menu is collapsed, expanding...")
                    customer_menu.click()
                    time.sleep(1.5)  # Wait for submenu to appear
            except:
                # Try clicking anyway
                try:
                    customer_menu.click()
                    time.sleep(1.5)
                except:
                    pass
            
            # Step 3: Find Customer Listing submenu item
            print("EnvironmentAccessAgent: Step 3 - Finding Customer Listing submenu item...")
            
            customer_listing_selectors = [
                'text=/Customer.*Listing/i',
                'text=/Listing/i',
                'text=Customer Listing',
                '[class*="submenu"]:has-text("Listing")',
                'a:has-text("Listing")',
                'button:has-text("Listing")',
                '[role="menuitem"]:has-text("Listing")'
            ]
            
            customer_listing = None
            for selector in customer_listing_selectors:
                try:
                    locator = target_page.locator(selector).first
                    if locator.is_visible(timeout=2000):
                        # Make sure it's related to Customer
                        parent_text = ""
                        try:
                            parent = locator.locator('..').locator('..')
                            parent_text = parent.inner_text(timeout=500)
                        except:
                            pass
                        
                        if 'customer' in parent_text.lower() or 'customer' in locator.inner_text(timeout=500).lower():
                            customer_listing = locator
                            print(f"EnvironmentAccessAgent: Found Customer Listing with selector: {selector}")
                            break
                except:
                    continue
            
            if not customer_listing:
                # Try to find all submenu items
                print("EnvironmentAccessAgent: Trying alternative method - searching all submenu items...")
                submenu_items = target_page.locator('[class*="submenu"] a, [class*="sub-menu"] a, .ant-menu-submenu a').all()
                for item in submenu_items:
                    try:
                        text = item.inner_text(timeout=500).strip()
                        if 'listing' in text.lower() or 'list' in text.lower():
                            customer_listing = item
                            print(f"EnvironmentAccessAgent: Found listing item: {text}")
                            break
                    except:
                        continue
            
            if not customer_listing:
                return {
                    'success': False,
                    'error': 'Could not find Customer Listing submenu item',
                    'timestamp': datetime.now().isoformat()
                }
            
            # Step 4: Click on Customer Listing
            print("EnvironmentAccessAgent: Step 4 - Clicking Customer Listing...")
            customer_listing.scroll_into_view_if_needed()
            time.sleep(0.5)
            customer_listing.click()
            
            # Step 5: Wait for navigation
            print("EnvironmentAccessAgent: Step 5 - Waiting for Customer Listing page to load...")
            target_page.wait_for_load_state('networkidle', timeout=30000)
            time.sleep(2)
            
            final_url = target_page.url
            page_title = target_page.title()
            
            print(f"EnvironmentAccessAgent: Successfully navigated to Customer Listing!")
            print(f"EnvironmentAccessAgent: URL: {final_url}")
            print(f"EnvironmentAccessAgent: Page title: {page_title}")
            
            return {
                'success': True,
                'url': final_url,
                'page_title': page_title,
                'message': 'Successfully navigated to Customer Listing',
                'timestamp': datetime.now().isoformat()
            }
            
        except Exception as e:
            import traceback
            error_trace = traceback.format_exc()
            print(f"EnvironmentAccessAgent: Error navigating to Customer Listing: {str(e)}")
            
            return {
                'success': False,
                'error': str(e),
                'error_trace': error_trace,
                'timestamp': datetime.now().isoformat()
            }
    
    def _study_submenu_in_context(self, page: 'Page') -> Dict[str, Any]:
        """
        Study submenu using an existing page context.
        Internal method used by access_and_study_submenu.
        """
        return self.study_submenu(page)


# Initialize EnvironmentAccessAgent instance
_environment_access_agent = None

def get_environment_access_agent(config: Dict[str, Any] = None) -> EnvironmentAccessAgent:
    """
    Get or create EnvironmentAccessAgent instance.
    
    Args:
        config: Configuration dictionary with credentials and URLs
    
    Returns:
        EnvironmentAccessAgent instance
    """
    global _environment_access_agent
    if _environment_access_agent is None:
        _environment_access_agent = EnvironmentAccessAgent(config=config)
    return _environment_access_agent


# Example usage
if __name__ == "__main__":
    # Initialize agent
    config = {
        'login_url': 'https://devapps.energo-pro.bg/app/portal/login/?uiLang=en_US',
        'username': 'n10610',
        'password': 'Start#2025'
    }
    agent = get_environment_access_agent(config=config)
    
    # Access DEV environment and study submenu
    result = agent.access_and_study_submenu('dev')
    print(f"\nAccess and Submenu Study Result: {result}\n")
    
    # Or access separately and then study
    # access_result = agent.access_environment('dev')
    # if access_result.get('success'):
    #     submenu_result = agent.study_submenu()
    #     print(f"\nSubmenu Structure: {submenu_result}\n")

