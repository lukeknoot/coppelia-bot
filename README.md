# coppelia-bot

[![Build Status](https://travis-ci.org/lukeknoot/coppelia-bot.svg?branch=main)](https://travis-ci.org/lukeknoot/coppelia-bot)

A purely functional somewhat "real-word app" using ZIO built for a friend to help with dance class bookings.

As a newbie ZIO user this was a chance to experiment and learn the library. This is open to critique and to learn from, though it is a specialised app and probably provides no direct value to anyone else.

Information about the [problem](https://github.com/lukeknoot/dance-auto-booking/wiki/Problem-Domain) this tries to solve and some [technical considerations](https://github.com/lukeknoot/dance-auto-booking/wiki/Technical-Considerations) addressed can be found in the wiki as a reference on how they can translate to implementation.

## Local Run 
This project supports [SBT Revolver](https://github.com/spray/sbt-revolver) and expects a valid `application.dev.conf` in the resources folder.
- `sbt reStart`

## Test
- `sbt test` 

## Deployment

### Build Image

- `sbt docker:publishLocal`

### Run container

In a directory with a valid `application.conf`:
- `docker_run.sh`
