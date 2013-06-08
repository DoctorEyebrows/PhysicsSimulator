package physicsSimulator;

class Prop extends Brush //represents an extended object obeying the laws of classical mechanics
{//TODO: investigate nullPointerException due to declaring a variable which is already initialized in the superclass
    public Prop(Vector origin) {
        super();
        createShape(origin);
        rotate(Math.random()*Math.PI*2);
        vel = new Vector(0, 0);
        angVel = 0;
        oneOverMass = 1.0/10;
        oneOverI = 1.0/10000;
        elasticity = 0.3;
        //offsetImpulse(new Vector(0,20),new Vector(20,0));
        PhysicsSimulator.world.registerObject(this); //note: "this" here is a reference to a prop, while "this" in Brush() is a reference to a brush. That's why I don't call super.
    }

    @Override
    public void drift(double t) {
        //moves the object to its new position t seconds later, assuming its velocity doesn't change in that time period
        COM.add(vel.mult(t));
        rotate(angVel*t);
    }
    
    @Override
    public void impulse(Vector p) {
        //adds impulse p to the prop's momentum, as though a brief force had acted on it during this frame
        vel.add(p.mult(oneOverMass));
    }
    
    public void offsetImpulse(Vector offset, Vector p) {
        //like impulse but takes the force's point of action (relative to this.COM) into account, resulting in a change in angVel
        impulse(p);
        angVel += (offset.cross(p) * oneOverI);
    }
    
    public void applyGravity(double t) {
        //accelerates the object a bit, corresponding to gravity
        vel.y += PhysicsSimulator.gravity * t;
    }
    
    @Override
    public void update(double t)
    {
        //props however, do do shit
        drift(t);
        //applyGravity(t); //acceleration due to gravitational field; g = 9.8 pixels/second^2 //can't do this until world has handled collisions
    }
}
