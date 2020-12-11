# coppelia-bot

A purely functional somewhat "real-word app" using ZIO built for a friend to help with dance class bookings.

As a newbie ZIO user this was a chance to experiment and learn the library. This is open to critique and to learn from, though it is a specialised app and probably provides no direct value to anyone else.

Information about the [problem](https://github.com/lukeknoot/dance-auto-booking/wiki/Problem-Domain) this tries to solve and some [technical considerations](https://github.com/lukeknoot/dance-auto-booking/wiki/Technical-Considerations) addressed can be found in the wiki as a reference on how they can translate to implementation.

## Local Run 
This project supports [SBT Revolver](https://github.com/spray/sbt-revolver)
- `sbt reStart`

## Test
- `sbt test` 

## Deployment

For production, this expects valid values in application.conf.

### Build Image
- `sbt docker:publishLocal`

### Run container
- `sudo docker run -d dance-auto-booking:<tag>`
