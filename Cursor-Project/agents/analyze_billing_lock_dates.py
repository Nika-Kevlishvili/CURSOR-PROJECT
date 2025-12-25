"""
Script to analyze creation dates of COMPLETED billing runs with remaining locks.

This script groups billing runs by creation date to identify patterns.
"""

import psycopg2
from datetime import datetime
from typing import List, Dict, Any
import sys
import os
from collections import defaultdict

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
ORDER BY b.create_date DESC
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


def analyze_by_date(results: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Analyze billing runs by creation date."""
    by_date = defaultdict(list)
    by_month = defaultdict(list)
    by_year_month = defaultdict(list)
    
    oldest = None
    newest = None
    
    for result in results:
        create_date = result['create_date']
        if isinstance(create_date, str):
            create_date = datetime.fromisoformat(create_date.replace('Z', '+00:00'))
        
        date_str = create_date.date().isoformat()
        month_str = create_date.strftime('%Y-%m')
        year_month_str = create_date.strftime('%Y-%m')
        
        by_date[date_str].append(result)
        by_month[month_str].append(result)
        by_year_month[year_month_str].append(result)
        
        if oldest is None or create_date < oldest:
            oldest = create_date
        if newest is None or create_date > newest:
            newest = create_date
    
    return {
        "by_date": dict(by_date),
        "by_month": dict(by_month),
        "oldest": oldest,
        "newest": newest,
        "total": len(results)
    }


def print_analysis(env_name: str, analysis: Dict[str, Any]):
    """Print date analysis results."""
    print(f"\n{'=' * 80}")
    print(f"Date Analysis for {env_name}")
    print(f"{'=' * 80}\n")
    
    if analysis["total"] == 0:
        print("✅ No billing runs with locks found.\n")
        return
    
    print(f"📊 Summary:")
    print(f"   Total billing runs with locks: {analysis['total']}")
    if analysis["oldest"]:
        print(f"   Oldest billing run: {analysis['oldest'].strftime('%Y-%m-%d %H:%M:%S')}")
    if analysis["newest"]:
        print(f"   Newest billing run: {analysis['newest'].strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # Group by month
    print(f"📅 Distribution by Month:")
    print(f"\n{'Month':<15} {'Count':<10} {'Total Locks':<15} {'Avg Locks':<15}")
    print("-" * 55)
    
    for month in sorted(analysis["by_month"].keys(), reverse=True):
        runs = analysis["by_month"][month]
        total_locks = sum(r['lock_count'] for r in runs)
        avg_locks = total_locks / len(runs) if runs else 0
        print(f"{month:<15} {len(runs):<10} {total_locks:<15} {avg_locks:.2f}")
    
    print()
    
    # Show daily breakdown for recent dates (last 30 days)
    print(f"📆 Daily Breakdown (Last 30 Days):")
    print(f"\n{'Date':<15} {'Count':<10} {'Total Locks':<15} {'Avg Locks':<15} {'Billing Numbers'}")
    print("-" * 100)
    
    sorted_dates = sorted(analysis["by_date"].keys(), reverse=True)[:30]
    for date_str in sorted_dates:
        runs = analysis["by_date"][date_str]
        total_locks = sum(r['lock_count'] for r in runs)
        avg_locks = total_locks / len(runs) if runs else 0
        billing_numbers = ", ".join([r['billing_number'] for r in runs[:3]])
        if len(runs) > 3:
            billing_numbers += f" ... (+{len(runs) - 3} more)"
        print(f"{date_str:<15} {len(runs):<10} {total_locks:<15} {avg_locks:.2f}      {billing_numbers}")
    
    print()
    
    # Show top 10 by lock count
    print(f"🔝 Top 10 Billing Runs by Lock Count:")
    print(f"\n{'Billing Number':<25} {'Date':<20} {'Lock Count':<15} {'Entity Types'}")
    print("-" * 100)
    
    sorted_by_locks = sorted(analysis["by_month"][list(analysis["by_month"].keys())[0]], 
                            key=lambda x: x['lock_count'], reverse=True)[:10]
    for run in sorted_by_locks:
        create_date = run['create_date']
        if isinstance(create_date, str):
            create_date = datetime.fromisoformat(create_date.replace('Z', '+00:00'))
        date_str = create_date.strftime('%Y-%m-%d %H:%M')
        entity_types = run['locked_entity_types'][:50] + "..." if len(run['locked_entity_types']) > 50 else run['locked_entity_types']
        print(f"{run['billing_number']:<25} {date_str:<20} {run['lock_count']:<15} {entity_types}")


def analyze_all_environments():
    """Analyze all environments for billing run creation dates."""
    print("=" * 80)
    print("Creation Date Analysis for COMPLETED Billing Runs with Remaining Locks")
    print("=" * 80)
    print(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    
    all_analyses = {}
    
    for env_name, config in ENVIRONMENTS.items():
        print(f"\n{'=' * 80}")
        print(f"Environment: {env_name}")
        print(f"Host: {config['host']}:{config['port']}")
        print(f"{'=' * 80}\n")
        
        conn = connect_to_database(env_name, config)
        if not conn:
            continue
        
        try:
            results = query_completed_billing_runs_with_locks(conn, env_name)
            analysis = analyze_by_date(results)
            all_analyses[env_name] = analysis
            print_analysis(env_name, analysis)
        except Exception as e:
            print(f"❌ Error processing {env_name}: {str(e)}\n")
        finally:
            conn.close()
    
    # Overall summary
    print("\n" + "=" * 80)
    print("OVERALL SUMMARY")
    print("=" * 80)
    
    total_all = sum(a["total"] for a in all_analyses.values())
    print(f"\nTotal billing runs with locks across all environments: {total_all}\n")
    
    for env_name, analysis in all_analyses.items():
        if analysis["total"] > 0:
            print(f"{env_name}:")
            print(f"  - Total: {analysis['total']}")
            if analysis["oldest"]:
                print(f"  - Oldest: {analysis['oldest'].strftime('%Y-%m-%d')}")
            if analysis["newest"]:
                print(f"  - Newest: {analysis['newest'].strftime('%Y-%m-%d')}")
            print()
    
    return all_analyses


if __name__ == "__main__":
    try:
        analyses = analyze_all_environments()
        sys.exit(0)
    except KeyboardInterrupt:
        print("\n\n⚠️  Interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Fatal error: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

