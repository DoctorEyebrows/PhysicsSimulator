package physicsSimulator;

import java.awt.*;

enum Prefabs {

    TRIANGLE, SQUARE, TRAPEZIUM
}

//remember: ORIGIN AT TOP-LEFT
class PhysicsSimulator {

    public static World world;
    public static Graphics g;
    public static String s;
    public static double gravity;

    public static void main(String[] args) throws InterruptedException //for some reason "throws InterruptedException" is necessary if you're to use Thread.sleep
    {
        s = "klsfjsklj";
        gravity = 100;
        world = new World();
        world.populate();
        //START MAIN LOOP
        double frameRate = 50;
        long frameDuration = 20000000; //(nanoseconds)
        long t0;
        while (true) {
            //calculate world t seconds from now:
            t0 = System.nanoTime();
            if(world.ticks < 0)continue;
            world.update(0.01); //TODO: make t a static variable of World
            //now draw it:
            world.draw();
            frameDuration = System.nanoTime() - t0;
            //nap time
            while (System.nanoTime() - t0 < 10000000) { //sleep for 10ms
                Thread.sleep(1);
            }
        }
    }
}