## Why

The platform stores consumers' CPF, phone and email in plain text with no legal basis, terms, or way to delete data. Establishments are controllers and the platform is the processor; sharing customers across grouped establishments needs an explicit basis and notice.

## What Changes

- Terms of Use and Privacy Policy (pt-BR), accepted by establishment owners at first login.
- Controller/processor statement (DPA) for establishments.
- Client anonymization/deletion (per account and person) and data export for the data subject.
- Consent/notice record for consumers, including group sharing.
- Basic audit log of access to and changes of personal data.

## Capabilities

### New Capabilities
- `data-protection`
### Modified Capabilities
- `client-identity`: deletion/anonymization and sharing consent.

## Impact

`Person`/`ClientAccount` services, new audit table, acceptance-tracking table, frontend legal pages/consent screen. Legal texts should be reviewed by a lawyer before selling.
Depends on: `mvp-02-client-identity`.
