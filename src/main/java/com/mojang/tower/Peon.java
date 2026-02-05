package com.mojang.tower;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import com.mojang.tower.event.EventBus;
import com.mojang.tower.event.DeathSound;
import com.mojang.tower.event.DingSound;
import com.mojang.tower.event.InfoPuffEffect;
import com.mojang.tower.movement.MovementRequest;
import com.mojang.tower.movement.MovementResult;
import com.mojang.tower.pathfinding.GridCell;
import com.mojang.tower.pathfinding.PathResult;
import com.mojang.tower.pathfinding.PathfindingService;
import com.mojang.tower.service.ServiceLocator;

public final class Peon extends Entity
{
    private static final int[] animSteps = { 0, 1, 0, 2 };
    private static final int[] animDirs = { 2, 0, 3, 1 };
    public double rot = 0;
    public double moveTick = 0;
    public int type;
    private int wanderTime = 0;
    private List<GridCell> currentPath;
    private int pathIndex;
    private double pathTargetX, pathTargetY;  // Track what target the path was computed for
    protected Job job;

    protected double xTarget, yTarget;

    private int hp = 100;
    private int maxHp = 100;
    private int xp = 0;
    private int nextLevel = 1;
    private int level = 0;

    public Peon(double x, double y, int type)
    {
        super(x, y, 1);
        this.type = type;
        rot = random.nextDouble() * Math.PI * 2;
        moveTick = random.nextInt(4 * 3);
    }

    public void init(Island island, Bitmaps bitmaps)
    {
        super.init(island, bitmaps);
        island.population++;
    }

    public void fight(Monster monster)
    {
        if (job == null && (type == 1 || random.nextInt(10) == 0))
        {
            setJob(new Job.Hunt(monster));
        }
        if (type == 0)
        {
            monster.fight(this);
            if ((hp -= 4) <= 0) die();
        }
        else
        {
            monster.fight(this);
            if (--hp <= 0) die();
        }
    }

    public void die()
    {
        EventBus.publish(new DeathSound());
        island.population--;
        if (type == 1)
        {
            island.warriorPopulation--;
        }
        alive = false;
    }

    public void setJob(Job job)
    {
        this.job = job;
        this.currentPath = null; // Clear path when job changes
        if (job != null) job.init(island, this);
    }

    public void tick()
    {
        if (job != null)
        {
            job.tick();
        }

        if (type == 1 || job == null) for (int i = 0; i < 15 && (job==null || job instanceof Job.Goto); i++)
        {
            TargetFilter monsterFilter = new TargetFilter()
            {
                public boolean accepts(Entity e)
                {
                    return e.isAlive() && (e instanceof Monster);
                }                  
            };
            Entity e = type == 0 ? getRandomTarget(30, 15, monsterFilter) : getRandomTarget(70, 80, monsterFilter);
            if (e instanceof Monster monster)
            {
                setJob(new Job.Hunt(monster));
            }
        }

        if (hp < maxHp && random.nextInt(5) == 0)
        {
            hp++;
        }
        /*        if (target == null || !target.isAlive() || random.nextInt(200) == 0)
                {
                    target = getRandomTarget();
                    if (!(target instanceof Tree))
                    {
                        target = null;
                    }
                }*/

        double speed = 1;
        if (wanderTime == 0 && job != null && job.hasTarget())
        {
            // Invalidate path if job target has moved significantly (>= 1 grid cell = 4 world units)
            if (currentPath != null) {
                double targetDx = job.xTarget - pathTargetX;
                double targetDy = job.yTarget - pathTargetY;
                if (targetDx * targetDx + targetDy * targetDy >= 16.0) {
                    currentPath = null;  // Target moved, recompute path
                }
            }

            // Check if already at target
            double xd = job.xTarget - x;
            double yd = job.yTarget - y;
            double rd = job.targetDistance + r;
            if (xd * xd + yd * yd < rd * rd)
            {
                job.arrived();
                currentPath = null;
                speed = 0;
            }
            else
            {
                // Need path to target
                if (currentPath == null) {
                    PathResult result = ServiceLocator.pathfinding().findPath(x, y, job.xTarget, job.yTarget);
                    switch (result) {
                        case PathResult.Found(var path) -> {
                            currentPath = path;
                            pathIndex = 1; // Skip first cell (current position)
                            pathTargetX = job.xTarget;
                            pathTargetY = job.yTarget;
                        }
                        case PathResult.NotFound(var reason) -> {
                            // No path found - will use random movement below
                            currentPath = null;
                        }
                    }
                }

                // Follow path if we have one
                if (currentPath != null && pathIndex < currentPath.size()) {
                    GridCell waypoint = currentPath.get(pathIndex);
                    double wx = PathfindingService.gridToWorldX(waypoint);
                    double wy = PathfindingService.gridToWorldY(waypoint);

                    // Check if close enough to waypoint
                    double wdx = wx - x;
                    double wdy = wy - y;
                    if (wdx * wdx + wdy * wdy < 4.0) {
                        pathIndex++;
                        if (pathIndex >= currentPath.size()) {
                            currentPath = null; // Path complete
                        }
                    } else {
                        rot = Math.atan2(wdy, wdx);
                    }
                } else if (currentPath == null) {
                    // No path available - random direction (same as original else branch)
                    rot += (random.nextDouble() - 0.5) * random.nextDouble() * 2;
                }
            }
        }
        else
        {
            // Original else branch: wanderTime > 0 OR no job OR job has no target
            rot += (random.nextDouble() - 0.5) * random.nextDouble() * 2;
        }

        if (wanderTime > 0) wanderTime--;

        speed += level * 0.1;

        double targetX = x + Math.cos(rot) * 0.4 * speed;
        double targetY = y + Math.sin(rot) * 0.4 * speed;
        MovementResult result = ServiceLocator.movement().move(
            new MovementRequest(this, targetX, targetY)
        );
        switch (result) {
            case MovementResult.Moved(var newX, var newY) -> {
                // Position already updated by MovementSystem
            }
            case MovementResult.Blocked(var blocker) -> {
                // Path is now invalid - clear and try again next tick
                currentPath = null;
                if (job != null) {
                    if (blocker != null) {
                        job.collide(blocker);
                    } else {
                        job.cantReach();
                    }
                }
                // Original behavior preserved exactly
                rot = random.nextDouble() * Math.PI * 2;
                wanderTime = random.nextInt(30) + 3;
            }
        }

        moveTick += speed;


        super.tick();
    }

    public void render(Graphics2D g, double alpha)
    {
        int rotStep = (int) Math.floor((rot - island.rot) * 4 / (Math.PI * 2) + 0.5);
        int animStep = animSteps[(int) (moveTick / 4) & 3];

        int x = (int) (xr - 4);
        int y = -(int) (yr / 2 + 8);

        int carrying = -1;
        if (job != null) carrying = job.getCarried();

        if (carrying >= 0)
        {
            g.drawImage(bitmaps.peons[2][animDirs[rotStep & 3] * 3 + animStep], x, y, null);
            g.drawImage(bitmaps.carriedResources[carrying], x, y - 3, null);
        }
        else
        {
            g.drawImage(bitmaps.peons[type][animDirs[rotStep & 3] * 3 + animStep], x, y, null);
        }

        if (hp < maxHp)
        {
            g.setColor(Color.BLACK);
            g.fillRect(x + 2, y - 2, 4, 1);
            g.setColor(Color.RED);
            g.fillRect(x + 2, y - 2, hp * 4 / maxHp, 1);
        }
        
        if (level>0)
        {
            
        }
    }

    public void setType(int i)
    {
        this.type = i;
        hp = maxHp = type == 0 ? 20 : 100;
    }

    public void addXp()
    {
        xp++;
        if (xp==nextLevel)
        {
            nextLevel = nextLevel*2+1;
            EventBus.publish(new InfoPuffEffect(x, y, 0));
            hp+=10;
            maxHp+=10;
            level++;
            EventBus.publish(new DingSound());
        }
    }
}