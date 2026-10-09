#!/bin/bash

# Script to map changed files to Playwright test tags
# Usage: ./map-tags.sh "comma,separated,changed,files"
#
# This script analyzes changed Java files in the Phoenix project and maps them
# to appropriate Playwright test tags based on domain/module organization.
#
# Tag mappings (by domain):
#   - Billing & Invoicing     → @billing
#   - Customer Management     → @customer
#   - Contracts & Orders      → @contractsAndOrders
#   - Products & Services     → @productsAndServices
#   - Receivables & Payments  → @receivableManagement
#   - CRM & Communications    → @crm
#   - Mass Imports            → @massImport
#   - Templates & Processes   → @operationsManagment
#
# Returns: Space-separated test tags or "SKIP" if no matches

CHANGED_FILES="$1"
TEST_TAGS=""

if [ -z "$CHANGED_FILES" ]; then
  echo "SKIP"
  exit 0
fi

# Function to add tag only if not already present
add_tag() {
  local tag="$1"
  if ! echo "$TEST_TAGS" | grep -q "$tag"; then
    TEST_TAGS="$TEST_TAGS $tag"
  fi
}

# Convert comma-separated list to lowercase for case-insensitive matching
FILES_LOWER=$(echo "$CHANGED_FILES" | tr '[:upper:]' '[:lower:]')

# ============================================================================
# BILLING DOMAIN
# Paths: service/billing/, repository/billing/, phoenix-billing-run/
# Keywords: billing, invoice, reversal, vat, scales, profile
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/billing|repository/billing|model.*billing|phoenix-billing-run|billingrun|invoice|billingcondition|billinggroup|billing.*controller|billing.*service"; then
  add_tag "@billing"
elif echo "$FILES_LOWER" | grep -qE "\bbilling\b.*\.(java|ts)|invoice.*\.(java|ts)"; then
  add_tag "@billing"
fi

# ============================================================================
# CUSTOMER DOMAIN
# Paths: service/customer/, repository/customer/, apis/
# Keywords: customer, private, legal, account manager, segment, indicators
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/customer|repository/customer|model/entity/customer|customer.*repository|customer.*service|customer.*controller"; then
  add_tag "@customer"
# APIS integration (customer identity verification)
elif echo "$FILES_LOWER" | grep -qE "apis/.*customer|apis.*service|apis.*model"; then
  add_tag "@customer"
elif echo "$FILES_LOWER" | grep -qE "\bcustomer\b.*\.(java|ts)"; then
  add_tag "@customer"
fi

# ============================================================================
# CONTRACTS & ORDERS DOMAIN
# Paths: service/contract/, repository/contract/
# Keywords: contract, order, termination, terms, action, penalty, express
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/contract|repository/contract|model/entity.*contract|productcontract|servicecontract|goodsorder|serviceorder"; then
  add_tag "@contractsAndOrders"
elif echo "$FILES_LOWER" | grep -qE "termination|action.*controller|action.*service|penalty|expresscontract"; then
  add_tag "@contractsAndOrders"
elif echo "$FILES_LOWER" | grep -qE "terms.*service|terms.*repository|termsgroup"; then
  add_tag "@contractsAndOrders"
elif echo "$FILES_LOWER" | grep -qE "\b(contract|order)\b.*\.(java|ts)"; then
  add_tag "@contractsAndOrders"
fi

# ============================================================================
# PRODUCTS & SERVICES DOMAIN
# Paths: service/product/, repository/product/
# Keywords: product, service, goods, price component, price parameter
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/product|repository/product|model/entity/product"; then
  add_tag "@productsAndServices"
elif echo "$FILES_LOWER" | grep -qE "pricecomponent|priceparameter|producttype|productgroup|serviceunit|servicetype"; then
  add_tag "@productsAndServices"
elif echo "$FILES_LOWER" | grep -qE "goods.*supplier|goods.*group|goods.*unit"; then
  add_tag "@productsAndServices"
elif echo "$FILES_LOWER" | grep -qE "\b(product|service|goods)\b.*\.(java|ts)" | grep -vqE "servicecontract|serviceorder"; then
  add_tag "@productsAndServices"
fi

# ============================================================================
# RECEIVABLES & PAYMENTS DOMAIN
# Paths: service/receivable/, repository/receivable/, phoenix-payment-api/
# Keywords: receivable, payment, deposit, rescheduling, offsetting, liability
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/receivable|repository/receivable|phoenix-payment-api|payment.*controller"; then
  add_tag "@receivableManagement"
elif echo "$FILES_LOWER" | grep -qE "payment.*service|receivable.*service|receivable.*repository|deposit|paymentpackage"; then
  add_tag "@receivableManagement"
elif echo "$FILES_LOWER" | grep -qE "rescheduling|offsetting|liability|interestrate|latepaymentfine|compensation"; then
  add_tag "@receivableManagement"
elif echo "$FILES_LOWER" | grep -qE "blocking|disconnection|reconnection|reminder|collection"; then
  add_tag "@receivableManagement"
elif echo "$FILES_LOWER" | grep -qE "\b(receivable|payment|liability)\b.*\.(java|ts)"; then
  add_tag "@receivableManagement"
fi

# ============================================================================
# CRM & COMMUNICATIONS DOMAIN
# Paths: service/crm/, repository/crm/, service/notifications/
# Keywords: crm, communication, sms, email, notification, contact
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/crm|repository/crm|service/notification|emailsender"; then
  add_tag "@crm"
elif echo "$FILES_LOWER" | grep -qE "customercommunication|customercontact|communicationtopic|communicationpurpose"; then
  add_tag "@crm"
elif echo "$FILES_LOWER" | grep -qE "\bcrm\b.*\.(java|ts)|communication.*\.(java|ts)"; then
  add_tag "@crm"
fi

# ============================================================================
# MASS IMPORTS DOMAIN
# Paths: service/massImport/, phoenix-mass-import/, mass-imports/
# Keywords: mass import, bulk import, excel import
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/massimport|phoenix-mass-import|mass-imports/|massimport.*service"; then
  add_tag "@massImport"
elif echo "$FILES_LOWER" | grep -qE "massimport.*generator|massimport.*payload"; then
  add_tag "@massImport"
fi

# ============================================================================
# OPERATIONS MANAGEMENT (Templates & Processes)
# Paths: service/template/, service/document/, process/
# Keywords: template, process, document, qes, workflow
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/template|service/document|process/|documentmerger"; then
  add_tag "@operationsManagment"
elif echo "$FILES_LOWER" | grep -qE "template.*service|document.*service|qes.*document|workflow"; then
  add_tag "@operationsManagment"
elif echo "$FILES_LOWER" | grep -qE "\b(template|process)\b.*\.(java|ts)"; then
  add_tag "@operationsManagment"
fi

# ============================================================================
# POD (Point of Delivery) - Often impacts multiple domains
# Paths: service/pod/, repository/pod/
# Note: POD changes may affect contracts, billing, and customers
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/pod|repository/pod|pointofdelivery|pod.*service|pod.*repository"; then
  add_tag "@contractsAndOrders"
  add_tag "@billing"
fi

# ============================================================================
# NOMENCLATURE & REFERENCE DATA
# Paths: service/nomenclature/, repository/nomenclature/
# Note: Nomenclature changes can affect multiple domains, add relevant tags
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/nomenclature|repository/nomenclature"; then
  # Nomenclature impacts most tests, but we'll be selective
  # Only add tags if specific nomenclatures are changed
  if echo "$FILES_LOWER" | grep -qE "profile|currency|region|district"; then
    add_tag "@customer"
    add_tag "@contractsAndOrders"
  fi
fi

# ============================================================================
# TASK & ACTIVITY MANAGEMENT
# Paths: service/task/, repository/task/, service/activity/
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/task|repository/task|service/activity|tasktype|systemactivity"; then
  add_tag "@operationsManagment"
fi

# ============================================================================
# RISK MANAGEMENT
# Paths: service/riskList/, RiskListController
# ============================================================================
if echo "$FILES_LOWER" | grep -qE "service/risklist|riskassessment|creditrating|risklistcontroller"; then
  add_tag "@customer"
fi

# ============================================================================
# CONFIGURATION & SECURITY (typically doesn't need test runs)
# Paths: config/, security/, exception/, util/
# ============================================================================
# Intentionally not mapping these to specific test tags unless they're
# business logic related. Add tags here if specific config changes require tests.

# ============================================================================
# INTEGRATION & EXTERNAL SYSTEMS
# ============================================================================
# xEnergie integration
if echo "$FILES_LOWER" | grep -qE "xenergie|service/xenergie"; then
  add_tag "@contractsAndOrders"
fi

# Online payment integration
if echo "$FILES_LOWER" | grep -qE "onlinepayment|epay"; then
  add_tag "@receivableManagement"
fi

# ============================================================================
# Trim leading/trailing spaces and remove duplicates
# ============================================================================
TEST_TAGS=$(echo "$TEST_TAGS" | tr ' ' '\n' | sort -u | tr '\n' ' ' | xargs)

if [ -z "$TEST_TAGS" ]; then
  echo "SKIP"
else
  echo "$TEST_TAGS"
fi
