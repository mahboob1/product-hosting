##### 1. Onboard to IDPS:
Use the [onboarding portal](https://github.intuit.com/pages/idps-ops/onboard). This creates a JIRA. When the onboarding is done you will receive an e-mail.

##### 2. Whitelist your NAT Gateway IPs
Whitelist your NAT Gateway IP addresses [here](https://github.intuit.com/pages/idps-ops/whitelist/). These IP addresses are available in the AWS Console Web UI via `VPC` -> `NAT Gateways` -> `Elastic IP Address` values.

#####  3. Get API Keys
1. Open the IDPS management portal: [Preprod](https://vkm-e2e.ps.idps.a.intuit.com) | [Prod](https://vkm.ps.idps.a.intuit.com)
2. Select API Keys
3. Create and download a new Application API Key and Management API Key. Both need to be read/write.
4. It is recommended to store these `.pem` files inside of `~/.idps`.

##### 4. Get your secrets:
- App Secret: Available via the DevPortal in the Credentials section.
- OIL (aka Splunk) can be downloaded [here](https://intuit.app.box.com/s/z6sog9qg3d94erb50tlnlmkakcaysuhj/folder/41232479530). Ask Mark Russell for the password, you will later be prompted for it when uploading secrets to IDPS. Place the pem files inside `~/.idps`. The pem that has `ppd` in the filename is for preprod, the pem with `prd` in the filename is for prod, the pem with `ca` in the filename is for both preprod and prod. Rename the certs so that they match the OIL file names in `ops/idps.yaml` 

##### 5. Create IDPS environment and load secrets:
Run `aws/scripts/idps-config.sh`.
It will prompt you for the location of your API Keys (pem files) and IDPS appliance endpoint **(be sure to include https:// prefix)**.
It will also prompt you for some of the secrets, such the OIL password, and your App Secret.

- Preprod management endpoint: `https://vkm-e2e.ps.idps.a.intuit.com`
- Prod management endpoint: `https://vkm.ps.idps.a.intuit.com`
- Example IDPS endpoint: `https://yourservice-PRE-PRODUCTION-A1B2C3.pd.idps.a.intuit.com`

If you experience errors, execute `rm ~/.idps/*.yaml` and rerun `aws/scripts/idps-config.sh`. 

##### 6. Update Chef roles with IDPS appliance and policy IDs:
Run `aws/scripts/idps-config.sh list-policies`.

Put the OIL policies and your IDPS endpoint in the Chef roles for all environments. Here is an example for `ops/chef/roles/qal.json` using the policy ID `oil-qal`. **IMPORTANT: Don't include https:// in the appliance endpoint.**

```
"api_policy_id": "p-a1b2c3d4e5f6",
"appliance": "yourservice-PRE-PRODUCTION-A1B2C3.pd.idps.a.intuit.com",
```

##### 7. Commit and deploy
Commit your changes to `ops/idps.yaml` and the `ops/chef/roles/{env}.json` files to the master branch and push to GitHub.

##### 8. Use secrets in your application
Retrieve the file paths of your secrets using `aws/scripts/idps-config.sh list-secrets`

Your application code can read secrets using the `app-{env}` policy IDs. Don't write Java code, instead refer to [jsk-sample-idps](https://github.intuit.com/services-config/jsk-sample-idps) to fetch the secrets at runtime. You do not need to onboard to CloudConfig. Simply include `jsk-spring-config-idps-client` in `app/pom.xml` and add these values to `bootstrap-{env}.properties`. Here is an example from [Settings Service](https://github.intuit.com/qbshared-settings/settings-service/blob/60801b5145a4a6e3dd2c8a4df23f3ccfef29adac/app/src/main/resources/bootstrap-qal.properties):

```
jsk.spring.config.idps.connection.endpoint=settings-PRE-PRODUCTION-NNK9MD.pd.idps.a.intuit.com
jsk.spring.config.idps.connection.policy-id=p-1pd0jzviyo3y
security.intuit.appSecret={secret}idps:/settings-service-app-secret
```
