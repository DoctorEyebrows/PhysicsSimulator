package physicsSimulator;

import java.awt.*;

public class DrawDot extends DrawLine //only really extended for polymorphism
{
    Vector v;
    
    public DrawDot(Vector v)
    {
        this.v = v;
    }
    
    public DrawDot(Vector v,Color c)
    {
        this.v = v;
        colour = c;
    }
    
    @Override
    public void draw(Graphics g)
    {
        if(colour != null)g.setColor(colour);
        g.fillOval((int)v.x-2,(int)v.y-2,4,4);
        g.setColor(Color.red);
    }
}