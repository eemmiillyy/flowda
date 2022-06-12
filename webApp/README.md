## Example web app for demo purposes

Consumes the output of the aggregation via Kafka and ddisplay on a web page.

## Development

`cd server`
`yarn`
`yarn ts-node index.ts`

In a new terminal
`cd cilent`
`yarn`
`yarn start`
(server needs to be running. Refresh until WS started. Will only see # AFTER an event has occured, A.K.A a database row was changed)
