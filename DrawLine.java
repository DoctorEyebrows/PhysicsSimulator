package physicsSimulator;

import java.awt.*;

class DrawLine {
    //utility class, exists to be drawn once in the next draw call and then discarded

    Vector v1, v2;
    boolean endpoints = false; //true if v2 defines the endpoint of the line, false if v2 defines the line to draw from the start point (v1)
    Color colour;

    public DrawLine(Vector v1, Vector v2) {
        this.v1 = v1;
        this.v2 = v2;
        endpoints = false;
    }

    public DrawLine(Vector v1, Vector v2, boolean endpoints) {
        this.v1 = v1;
        this.v2 = v2;
        this.endpoints = endpoints;
    }
    
    public DrawLine(Vector v1, Vector v2,Color colour) {
        this.v1 = v1;
        this.v2 = v2;
        endpoints = false;
        this.colour = colour;
    }

    public DrawLine(Vector v1, Vector v2, boolean endpoints, Color colour) {
        this.v1 = v1;
        this.v2 = v2;
        this.endpoints = endpoints;
        this.colour = colour;
    }
    
    public DrawLine(){} //for implicit calls from DrawDot

    public void draw(Graphics g) {
        if(colour != null)g.setColor(colour);
        if (endpoints) {
            v2.minus(v1).draw(g, v1);
        } else {
            v2.draw(g, v1);
        }
        g.setColor(Color.red);
    }
}
