A basic rigid body dynamics simulator, made for fun during my first year at Durham.

It can handle collisions between moving rigid, objects, and that's about it. Uses separating axis theorem for collision detection. Currently there's no such thing as friction, so collisions do look a bit strange sometimes. Also, resting contact is not handled properly (it requires some very different algorithms to collision handling), so objects will fall through each other sometimes.

Usage: press R to reset the simulator.