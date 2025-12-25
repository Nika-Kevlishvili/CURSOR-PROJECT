"""
Script to check all environments for COMPLETED billing runs with remaining locks.

This script queries all environments (Test, Dev, Dev2, PreProd) to find billing runs
with status COMPLETED that still have locked objects.
"""

import psycopg2
from datetime import datetime
from typing import List, Dict, Any
import sys
import os

# Fix Windows console encoding for emojis
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')

# Database configurations for all environments
ENVIRONMENTS = {
    "Test": {
        "host": "10.236.20.24",
        "port": 5432,
        "database": "phoenix",
        "user": "postgres",
        "password": "U&Vd2Ge@nyM1"
    },
    "Dev": {
        "host": "10.236.20.21",
        "port": 5432,
        "database": "phoenix",
        "user": "postgres",
        "password": "rakX5thB2qO3"
    },
    "Dev2": {
        "host": "10.236.20.22",
        "port": 5432,
        "database": "phoenix",
        "user": "postgres",
        "password": "uj6MfFgUkV1R"
    },
    "PreProd": {
        "host": "10.236.20.76",
        "port": 5432,
        "database": "phoenix",
        "user": "postgres",
        "password": "U&Vd2Ge@nyM1"
    }
}

QUERY = """
SELECT 
    b.id AS billing_run_id,
    b.billing_number,
    b.status::text AS status,
    b.create_date,
    b.modify_date,
    COUNT(l.id) AS lock_count,
    STRING_AGG(DISTINCT l.entity_type, ', ' ORDER BY l.entity_type) AS locked_entity_types
FROM billing.billings b
INNER JOIN lock.locks l ON l.billing_id = b.id
WHERE b.status::text = 'COMPLETED'
GROUP BY b.id, b.billing_number, b.status, b.create_date, b.modify_date
ORDER BY b.id DESC
"""

ADDITIONAL_STATS_QUERY = """
SELECT 
    COUNT(DISTINCT b.id) AS total_completed_billing_runs,
    COUNT(l.id) AS total_locks_for_completed_runs
FROM billing.billings b
LEFT JOIN lock.locks l ON l.billing_id = b.id
WHERE b.status::text = 'COMPLETED'
"""


def connect_to_database(env_name: str, config: Dict[str, Any]):
    """Connect to a PostgreSQL database."""
    try:
        conn = psycopg2.connect(
            host=config["host"],
            port=config["port"],
            database=config["database"],
            user=config["user"],
            password=config["password"]
        )
        return conn
    except Exception as e:
        print(f"❌ Error connecting to {env_name}: {str(e)}")
        return None


def query_completed_billing_runs_with_locks(conn, env_name: str) -> List[Dict[str, Any]]:
    """Query for COMPLETED billing runs with locks."""
    try:
        cursor = conn.cursor()
        cursor.execute(QUERY)
        
        results = []
        for row in cursor.fetchall():
            results.append({
                "billing_run_id": row[0],
                "billing_number": row[1],
                "status": row[2],
                "create_date": row[3],
                "modify_date": row[4],
                "lock_count": row[5],
                "locked_entity_types": row[6]
            })
        
        cursor.close()
        return results
    except Exception as e:
        print(f"❌ Error querying {env_name}: {str(e)}")
        return []


def get_additional_stats(conn, env_name: str) -> Dict[str, Any]:
    """Get additional statistics about completed billing runs."""
    try:
        cursor = conn.cursor()
        cursor.execute(ADDITIONAL_STATS_QUERY)
        row = cursor.fetchone()
        
        stats = {
            "total_completed_billing_runs": row[0] if row else 0,
            "total_locks_for_completed_runs": row[1] if row else 0
        }
        
        cursor.close()
        return stats
    except Exception as e:
        print(f"❌ Error getting stats for {env_name}: {str(e)}")
        return {"total_completed_billing_runs": 0, "total_locks_for_completed_runs": 0}


def check_all_environments():
    """Check all environments for COMPLETED billing runs with locks."""
    print("=" * 80)
    print("Checking All Environments for COMPLETED Billing Runs with Remaining Locks")
    print("=" * 80)
    print(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    
    all_results = {}
    summary = []
    
    for env_name, config in ENVIRONMENTS.items():
        print(f"\n{'=' * 80}")
        print(f"Environment: {env_name}")
        print(f"Host: {config['host']}:{config['port']}")
        print(f"{'=' * 80}\n")
        
        conn = connect_to_database(env_name, config)
        if not conn:
            all_results[env_name] = {
                "success": False,
                "error": "Connection failed",
                "results": [],
                "stats": {}
            }
            continue
        
        try:
            # Get statistics
            stats = get_additional_stats(conn, env_name)
            
            # Query for completed billing runs with locks
            results = query_completed_billing_runs_with_locks(conn, env_name)
            
            all_results[env_name] = {
                "success": True,
                "results": results,
                "stats": stats
            }
            
            # Print results
            print(f"📊 Statistics:")
            print(f"   Total COMPLETED billing runs: {stats['total_completed_billing_runs']}")
            print(f"   Total locks for COMPLETED runs: {stats['total_locks_for_completed_runs']}")
            print()
            
            if results:
                print(f"⚠️  Found {len(results)} COMPLETED billing run(s) with remaining locks:\n")
                for result in results:
                    print(f"   Billing Run ID: {result['billing_run_id']}")
                    print(f"   Billing Number: {result['billing_number']}")
                    print(f"   Status: {result['status']}")
                    print(f"   Create Date: {result['create_date']}")
                    print(f"   Modify Date: {result['modify_date']}")
                    print(f"   Lock Count: {result['lock_count']}")
                    print(f"   Locked Entity Types: {result['locked_entity_types']}")
                    print()
            else:
                print("✅ No COMPLETED billing runs with remaining locks found.\n")
            
            summary.append({
                "environment": env_name,
                "host": config["host"],
                "total_completed": stats["total_completed_billing_runs"],
                "completed_with_locks": len(results),
                "total_locks": stats["total_locks_for_completed_runs"]
            })
            
        except Exception as e:
            print(f"❌ Error processing {env_name}: {str(e)}\n")
            all_results[env_name] = {
                "success": False,
                "error": str(e),
                "results": [],
                "stats": {}
            }
        finally:
            conn.close()
    
    # Print summary
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print(f"\n{'Environment':<15} {'Host':<20} {'Total Completed':<20} {'With Locks':<15} {'Total Locks':<15}")
    print("-" * 80)
    
    for item in summary:
        print(f"{item['environment']:<15} {item['host']:<20} {item['total_completed']:<20} {item['completed_with_locks']:<15} {item['total_locks']:<15}")
    
    # Check if any issues found
    total_with_locks = sum(item['completed_with_locks'] for item in summary)
    
    print("\n" + "=" * 80)
    if total_with_locks > 0:
        print(f"⚠️  WARNING: Found {total_with_locks} COMPLETED billing run(s) with remaining locks across all environments!")
    else:
        print("✅ SUCCESS: No COMPLETED billing runs with remaining locks found in any environment.")
    print("=" * 80)
    
    return all_results


if __name__ == "__main__":
    try:
        results = check_all_environments()
        
        # Exit with error code if issues found
        total_issues = sum(
            len(env_results.get("results", []))
            for env_results in results.values()
            if env_results.get("success", False)
        )
        
        sys.exit(0 if total_issues == 0 else 1)
        
    except KeyboardInterrupt:
        print("\n\n⚠️  Interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Fatal error: {str(e)}")
        sys.exit(1)

