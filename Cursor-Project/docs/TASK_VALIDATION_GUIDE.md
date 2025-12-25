# Task Description Validation Guide

## ✅ Yes, I can validate task descriptions!

**PhoenixExpert** agent can:
- ✅ Analyze task descriptions
- ✅ Check Confluence for actual requirements
- ✅ Check code for actual implementation
- ✅ Compare and assess how well they match
- ✅ Provide detailed analysis and recommendations

---

## How It Works

### Process

When you provide a task description, I:

1. **First** - Check Confluence using MCP tools:
   - Search for relevant documentation
   - Check business rules and requirements
   - Find API documentation and specifications

2. **Second** - Check Phoenix codebase:
   - Search for controllers, services, models
   - Check validation rules
   - Find endpoints and business logic
   - Check actual implementation

3. **Then** - Make comparisons:
   - How well the task description matches Confluence
   - How well the task description matches the code
   - What is correct and what can be improved

4. **Finally** - Provide detailed assessment:
   - ✅ What is correct
   - ⚠️ What can be improved
   - ❌ What doesn't match
   - 📝 Recommendations

---

## Examples

### Example 1: Simple Task Assessment

**Your task description:**
```
"Add validation for customer email to be unique"
```

**My assessment:**
1. ✅ **Confluence check**: Search for customer validation rules
2. ✅ **Code check**: Check Customer entity and validation annotations
3. ✅ **Comparison**: Check if unique constraint already exists
4. 📊 **Assessment**: 
   - Is the requirement correct
   - Does it match the code
   - What needs to be done

### Example 2: Complex Task Assessment

**Your task description:**
```
"Create new endpoint /api/billing/calculate which:
- Takes customerId and period
- Returns billing amount
- Checks permissions
- Logs all requests"
```

**My assessment:**
1. ✅ **Confluence**: Check billing API documentation
2. ✅ **Code**: Search for BillingController, BillingService, existing endpoints
3. ✅ **Comparison**: 
   - Does a similar endpoint exist
   - What permissions are required
   - How requests are logged
4. 📊 **Assessment**:
   - What is correct in the requirements
   - What doesn't match existing code
   - What needs to be added
   - Recommendations for implementation

---

## What You'll Get

### 📊 Assessment Structure

```markdown
## Task Description Assessment

### ✅ Confluence Compliance
- [Details of what was found in Confluence]
- [How well it matches]

### ✅ Code Compliance  
- [Details of what was found in code]
- [How well it matches]

### 📋 Detailed Analysis
- ✅ Correct parts
- ⚠️ Parts that can be improved
- ❌ Non-compliant parts

### 💡 Recommendations
- [What should be changed]
- [What should be added]
```

---

## How to Use

### Method 1: Directly in Cursor Chat

Simply write:

```
Assess this task description:
[Your task description]

And compare with Confluence and code.
```

**I will automatically:**
1. Use PhoenixExpert
2. Check Confluence using MCP tools
3. Check Phoenix codebase
4. Give you detailed assessment

### Method 2: Specific Question

```
Assess this task description and tell me:
1. Does it match Confluence?
2. Does it match the code?
3. What validation rules are needed?
4. What endpoints already exist?
```

---

## What I Can Validate

### ✅ I can validate:

1. **API Endpoints**:
   - Whether endpoint exists
   - Whether path and method are correct
   - Whether it matches Confluence documentation

2. **Validation Rules**:
   - What validation rules are needed
   - Whether they match existing rules in code
   - What needs to be added

3. **Business Logic**:
   - How business logic works in code
   - Whether it matches Confluence description
   - What changes are needed

4. **Permissions & Security**:
   - What permissions are needed
   - How they are implemented in code
   - Whether they match security requirements

5. **Data Models**:
   - What fields are needed
   - Whether they match Entity classes
   - What relationships are needed

---

## Example Full Assessment

### Task description:
```
"Add phone number validation for customer to be in Georgia format"
```

### Assessment:

#### ✅ Confluence Check
- **Found**: Customer API Documentation
- **Specified**: Phone number format requirements
- **Compliance**: ✅ 100% matches

#### ✅ Code Check
- **Found**: `Customer.java` entity
- **Existing validation**: `@Pattern` annotation for phone
- **Compliance**: ⚠️ 70% - existing validation doesn't check Georgia format

#### 📋 Detailed Analysis

**✅ Correct:**
- Requirement is correct and matches Confluence
- Phone number validation exists in code

**⚠️ Can be improved:**
- Existing `@Pattern` annotation doesn't check Georgia format
- Need to update regex pattern

**❌ Non-compliant:**
- None

#### 💡 Recommendations

1. **Update `@Pattern` annotation** in `Customer.java`:
   ```java
   @Pattern(regexp = "^\\+995[0-9]{9}$", message = "Phone must be in Georgia format (+995XXXXXXXXX)")
   private String phoneNumber;
   ```

2. **Add unit test** for validation

3. **Update Confluence documentation** for new format

---

## Conclusion

### ✅ **Yes, I can validate task descriptions!**

When you provide a task description, I:

1. ✅ Check Confluence (fresh data via MCP)
2. ✅ Check Phoenix codebase
3. ✅ Make comparisons
4. ✅ Give you detailed assessment

**Simply write:**
```
Assess this task description: [Your task description]
```

And I will give you a complete analysis! 🎯

---

**Last Updated**: 2025-01-14

