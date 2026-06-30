# Qes Signing Flow

**Domain:** Billing / Invoicing
**Source:** QES Signing Flow.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 1 |
| Process Steps | 45 |
| Save Operations | 7 |
| Error States | 2 |

## User Actions (Entry Points)

- Start Signing

## Process Steps

- Signed
- Skip
- Documents Transferring to To be signed Sub Folder
- Select QES Signing Page
- Find Folder with the FIRST process identifier and link this PDF to the selected document
- Successfull
- Find Folder with the process identifier and link this PDF to the selected document
- Continue Process
- Transferring Done
- Successful
- Select Certificate with which you want to sign
- Phoenix Portal
- Signing Quantity > 1
- Make Selected Documents Inactive
- Local PC
- Computer Damage
- Reconnect
- Copies of Documents
- etc...
- Make Signed Document Row Active AND Change status
- Closing JSign
- Shared Folder
- Lost
- Authorize in JSign PDF
- Stop Signing
- Fully Signed
- QES Signing
- Transferring Document from Signed Sub Folder to Local Server
- Signed Documents
- Select documents
- Lose Connection
- Authorize in Phoenix
- Sign one Document
- Initiate Opening Jsign PDF and tell Signing Logics
- Send it to Signed sub folder
- Partly Signed
- Take Document from To be signed sub folder
- Signed Quantity < Signing Quantity
- Internet Disconnection
- Phoenix
- Signing Quantity = 1
- Retry connection during 5 mins
- JSign PDF
- Signed Quantity = Signing Quantity
- Closing Browser/Tab

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Delete To be signed
- Kill Process, Delete To be signed Sub Folder, Transfer Signed Docs back and delete Signed Sub Folder
- Find other process identifiers' Folders and delete them
- Find Signed Documents and Delete them
- Delete To be signed Sub Folder, Transfer Signed docs back and delete Signed Sub Folder
- Kill Process, Delete To be signed Sub Folder
- Copy and Create Folder for Signing Process with selected Documents

## Error/Exception States

- Cancel Signing
- Error

## Flow Connections

Direct relationships between steps:

- Start Signing --> Copy and Create Folder for Signing Process with selected Documents
- Authorize in Phoenix --> Select QES Signing Page
- Select QES Signing Page --> Select documents
- Select documents --> Start Signing
- Copy and Create Folder for Signing Process with selected Documents --> Make Selected Documents Inactive
- Make Selected Documents Inactive --> Documents Transferring to To be signed Sub Folder
- Transferring Document from Signed Sub Folder to Local Server --> Make Signed Document Row Active AND Change status
- Transferring Done --> Initiate Opening Jsign PDF and tell Signing Logics
- Authorize in JSign PDF --> Take Document from To be signed sub folder
- Send it to Signed sub folder --> Transferring Document from Signed Sub Folder to Local Server
- Take Document from To be signed sub folder --> Select Certificate with which you want to sign
- Select Certificate with which you want to sign --> Sign one Document
- Initiate Opening Jsign PDF and tell Signing Logics --> Authorize in JSign PDF
- Cancel Signing --> Find Signed Documents and Delete them
- Select documents --> Cancel Signing
- Find Folder with the FIRST process identifier and link this PDF to the selected document --> Find other process identifiers' Folders and delete them
- Partly Signed --> Select documents
- Fully Signed --> Select documents

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

