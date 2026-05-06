# IAM policy — utility-alerts endpoint

The `GET /api/assets/utility-alerts` endpoint reads three DynamoDB
tables owned by the EIF forecast pipeline (`Z:\EIF\h2o_training`).
asset-service already has write access to `esp32_alerts` and `esp32_thresholds`
and `esp32_asset_map`; this feature only adds **read** on two more:

- `esp32_usage_agg` — daily consumption aggregates (future: 30-day chart drawer)
- `esp32_forecast` — monthly Prophet output (`usedSoFar`, `totalEstimate`, `daysLeft`)

## Additive policy statement

Attach the following statement to the asset-service IAM role
(`isums-asset-service` in ECS / EC2 instance profile, or the IAM user
for dev). Do NOT replace existing statements — merge with the current
policy.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UtilityAlertsReadEifTables",
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:BatchGetItem",
        "dynamodb:DescribeTable"
      ],
      "Resource": [
        "arn:aws:dynamodb:ap-southeast-1:*:table/esp32_usage_agg",
        "arn:aws:dynamodb:ap-southeast-1:*:table/esp32_usage_agg/index/*",
        "arn:aws:dynamodb:ap-southeast-1:*:table/esp32_forecast",
        "arn:aws:dynamodb:ap-southeast-1:*:table/esp32_forecast/index/*"
      ]
    }
  ]
}
```

## Why no write access?

The EIF pipeline is the sole writer of both tables — asset-service
reading them directly keeps ownership clear and avoids the classic
"who invalidates the cache" ambiguity. If asset-service ever needs to
trigger a forecast refresh, use the existing `LambdaClient` invoke
against `esp32-forecast-dispatcher` (already wired in `IotForecastServiceImpl`).

## Credential source

asset-service resolves credentials via the standard AWS SDK provider
chain (`DefaultCredentialsProvider` in `DynamoConfig.java`), which
means:

- **Prod (ECS)**: task role — no env vars, no rotation needed.
- **Dev (workstation)**: `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY`
  env vars, or `~/.aws/credentials` default profile.

Region is pinned to `ap-southeast-1` in `DynamoConfig.java` — same
region as the EIF pipeline.

## Verification

After rollout, tail CloudWatch metrics for
`AWS/DynamoDB:ConsumedReadCapacityUnits` on both tables. Expected
profile: small burst on cache-miss (every 5 min per landlord), ~0
otherwise. If reads exceed the table's on-demand baseline, either
(a) bump the cache TTL, or (b) move to a DAX accelerator.
