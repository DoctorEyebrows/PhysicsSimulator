package physicsSimulator;

import java.util.ArrayList;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

class World {

    private JFrame frame;
    private MyPanel contentPane;
    private ArrayList<Brush> objects;
    private ArrayList<Brush> brushes;
    private ArrayList<Prop> props;
    
    static public ArrayList<DrawLine> drawLines;
    
    public ArrayList<Brush> intersectors;
    
    int ticks;

    public World() {
        objects = new ArrayList<Brush>();
        brushes = new ArrayList<Brush>();
        props = new ArrayList<Prop>();
        drawLines = new ArrayList<DrawLine>();
        intersectors = new ArrayList<Brush>();
        ticks = 0;

        //Set up the JFrame (that's a window)
        frame = new JFrame("Another physics simulator"); //TODO: put all this shit in a separate display class, it probably doesn't belong here
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setBackground(Color.BLACK);

        contentPane = new MyPanel();
        frame.setContentPane(contentPane);

        frame.setVisible(true);
        System.out.println("Done");
    }

    public void populate() {
        //Set up the world
        new Brush(new Vector(200, 200));
        Prop p = new Prop(new Vector(200, 40));
        //p.angVel = 0.8;
        /*p.rotate(Math.PI);
        Prop p = new Prop(new Vector(50,100));
        p.vel.x = 50;
        p.vel.y = -25;*/
    }
    
    public void restart() {
        objects.clear();
        props.clear();
        brushes.clear();
        drawLines.clear();
        populate();
        ticks = 0;
    }

    public void update(double t) {
        try{
        System.out.println("Tick "+ticks+":");
        Vector r = new Vector(contentPane.getMousePosition());
        
        for (Brush obj : objects) {
            //if(r != null) prop.COM = r.clone(); //move prop to mouse position
            obj.update(t);
            ///////////////////////////testing contains method /////////////////////////:
            if(obj.enclosesPoint(r)){drawLines.add(new DrawDot(new Vector(40,20),Color.pink)); break;} /////////////////////////
        }
        
        intersectors.clear();
        System.out.println(intersectors.size());
        box_intersections();
        System.out.println(intersectors.size());
        for(int i=0; i<intersectors.size(); i+=2){
            System.out.println("Boxes are intersecting!");
            Brush a = intersectors.get(i);
            Brush b = intersectors.get(i+1);
            if (a.SAT(b))
            {
                System.out.println("Intersection");
                //now do SCIENCE
                handleCollision(a,b,t);
            }
            else
                System.out.println("No intersection");
        }
        
        for (Prop obj : props) {
            obj.applyGravity(t);
        }
        
        ticks++;
        }
        catch (java.util.ConcurrentModificationException e) {System.out.println("Warning: swing's being a bitch!");}
    }
    
    public void handleCollision(Brush a, Brush b, double t)
    {
        //binary search the objects back in time until they're just touching
        a.drift(-t/2); b.drift(-t/2);
        t /= 2;
        int iterations=1;
        while(true){
            if(a.SAT(b)){if(iterations>=5)break; a.drift(-t/2); b.drift(-t/2);}
            else
            {a.drift(t/2); b.drift(t/2);}
            iterations++;
            t /= 2;
        }
        //Objects are now close enough to where they were at the moment of collision
        //Now figure out exactly where the collision occurred, and make sure that a is the object with the colliding face, and b wtih the colliding point:
        Brush swapVar = null;
        Vector pos = a.COM.plus(a.polygon.offset);
        for(int i=0; i<a.polygon.length; i++){
            if(b.enclosesPoint(pos)){swapVar=a; a=b; b=swapVar; break;} //a point of a is in b, so a has the colliding point and b has the colliding face
            pos.add(a.polygon.shape[i]);
        }
        if(swapVar == null){ //the colliding point must be in b, and a must have the colliding face, so no swap is needed, but we still need to find the collision point
            pos = b.COM.plus(b.polygon.offset);
            for(int i=0; i<b.polygon.length; i++){
                if(a.enclosesPoint(pos))break;
                pos.add(b.polygon.shape[i]);
            }
        }
        //pos should now be the absolute position of the collision
        //now we have to determine which side of which shape the point collided with
        double minDist = 10000;
        double sinLength;
        double cosLength;
        Vector corner = a.COM.plus(a.polygon.offset);
        Vector s = corner; //corner used as a placeholder, because java needs to know the variable gets initialized
        Vector cornerToP;
        Vector collSide = corner; //side involved in the collision (closest to the colliding point). As with s, corner is an arbitrary placeholder
        for(int i=0; i<a.polygon.length; i++){
            s = a.polygon.shape[i];
            cornerToP = pos.minus(corner);
            sinLength = Math.abs(s.sinLength(cornerToP));
            cosLength = s.cosLength(cornerToP);
            if(cosLength > 0 && cosLength < s.len() && sinLength < minDist){
                minDist = sinLength;
                collSide = s;
            }
            corner.add(s);
        }
        //s should now be the side which is involved in the collision
        Vector n = collSide.rot90();
        World.drawLines.add(new DrawDot(a.COM,Color.white));
        //now apply an offset force to both objects, acting at point pos, in direction n, with magnitude sufficient to prevent objects clipping and dependent on object elasticities
        double e = a.elasticity*b.elasticity;
        Vector r_a = pos.minus(a.COM);
        Vector r_b = pos.minus(b.COM);
        Vector u_a = a.vel.plus(r_a.rot90().mult(a.angVel));
        Vector u_b = b.vel.plus(r_b.rot90().mult(b.angVel));
        

        double numerator = u_a.minus(u_b).dot(n);
        if(numerator > 0.01)numerator *= (1+e); //effectively setting e to 0 when the objects are colliding very slowly, i.e. during resting contact
        double denominator = (n.dot(n)*(a.oneOverMass+b.oneOverMass));
        double gammaB = r_b.rot90().mult(b.oneOverI*r_b.cross(n)).dot(n);
        double intermediate = a.oneOverI*r_a.cross(n);
        double gammaA = r_a.rot90().mult(intermediate).dot(n);
        denominator += gammaA + gammaB;
        double lambda = numerator / denominator;
        System.out.println(numerator);
        System.out.println(denominator);
        System.out.println(lambda);
        n.scale(lambda);
        World.drawLines.add(new DrawLine(pos,n,Color.GREEN));
        a.offsetImpulse(r_a,n.mult(-1)); b.offsetImpulse(r_b,n);
        //a.impulse(n.mult(-1)); b.impulse(n);
        a.drift(0.01-t); b.drift(0.01-t);
        //ticks = -2;
    }

    public void box_intersections() {
        //returns a list of pairs of objects whose bounding boxes are intersecting
        for (int i = 0; i < (objects.size() - 1); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                Brush obji = objects.get(i);
                Brush objj = objects.get(j);
                if(obji.box_intersection(objj)){ //obji and objj's bounding boxes overlap*/ 
                    intersectors.add(obji);
                    intersectors.add(objj);
                }
            }
        }
    }

    public void registerObject(Brush brush) {
        objects.add(brush);
        brushes.add(brush);
    }

    public void registerObject(Prop prop) {
        objects.add(prop);
        props.add(prop);
    }

    public void draw() {
        frame.repaint();
    }

    class MyPanel extends JPanel implements MouseListener, KeyListener{

        private Vector brushOrigin;

        public MyPanel()
        {
            super();
            addMouseListener(this);
            addKeyListener(this);
            setFocusable(true);
        }
        
        public void mousePressed(MouseEvent mouse)
        {
            World.drawLines.add(new DrawDot(new Vector(40,40),Color.white));
        }
        
        public void mouseEntered(MouseEvent mouse){ } ////////////// ////////////////////////////////////////// 
        public void mouseExited(MouseEvent mouse){ }  //Necessary to implement MouseListener interface,
        public void mouseClicked(MouseEvent mouse){ } //but not used.
        public void mouseReleased(MouseEvent mouse){ }/////////////////////////////////////////////////////////
        
        public void keyTyped(KeyEvent key){ }
        public void keyReleased(KeyEvent key){ }
        
        public void keyPressed(KeyEvent key)
        {
            if(key.getKeyCode() == KeyEvent.VK_R){
                restart();
            }
        }
        
        @Override
        public void paintComponent(Graphics g) {
            //draw the brushes
            for (Brush brush : objects) {
                g.setColor(Color.WHITE);
                Vector COM = brush.COM;
                if (brush.drawTriangles) {
                    for (Triangle tri : brush.triangles) {
                        tri.draw(g, COM);
                    }
                }  {
                    g.setColor(Color.BLUE);
                    brush.polygon.draw(g, COM);
                }
                if (brush.drawCOM) {
                    g.setColor(Color.BLUE);
                    g.drawLine((int) COM.x, (int) COM.y, (int) COM.x, (int) COM.y); //draw a dot on the COM
                }
                double[] box = brush.boundingBox;
                if (brush.drawBoundingBox) {
                    g.setColor(Color.RED);
                    g.drawRect((int) (COM.x + box[0]), (int) (COM.y + box[2]), (int) (box[1] - box[0]), (int) (box[3] - box[2]));
                    //boundingBox really should be a class rather than an array
                }
            }
            for(DrawLine line : drawLines)line.draw(g);
            drawLines.clear();
        }
    }
}
