* GrapheneDB code test

The goal of this test is to implement a minimal HTTP API to provision,
de-provision and get information about a deployed database within a public cloud
provider, i.e. AWS. This is quite similar to our infrastructure and it will give
you a glimpse into some of the problems that you will work on. This test will help
us to understand better your way of thinking.

In order to simplify this test, state could be stored in memory, i.e. using just
a simple hash-map or whatever data structure that is suitable for the task.

Deployed databases should be accessible from the public internet, but design the
system taking into account different security practices.

There are many ways to deploy a database within the compute resources, you could
bake an AMI using tools such as Packer, you could use Docker, cloud-init, etc,
each one has advantages and disadvantages. We would like to know how do you
tackle those trade-offs.

** Database

To make it simple, you should deploy [[https://coreos.com/etcd/][etcd]] that is distributed as a statically
linked binary. Deploy it as a single node cluster with default configuration.

** Authentication

User management is out of the scope of this test, as a proposal you could store
a few tokens in-memory, think about how to secure them. As an alternative, you
could use an authentication SaaS such as Authy.

** API examples

The next listings show an example of how the API could look like, but you are
free to implement it as you think it is better.

#+BEGIN_SRC 
POST /databases

Content-Type: application/json
X-AUTH: random123token

{"name": "name",
 "plan": "t2.micro" // to simplify, just use a direct mapping between cloud provider instances.
}
#+END_SRC

#+BEGIN_SRC 
GET /databases/<id>

X-AUTH: random123token

{...}
#+END_SRC

#+BEGIN_SRC 
DELETE /databases/<id>

X-AUTH: random123token
#+END_SRC

** IaaS provider

Our platform runs on top of AWS but we understand that most of the core concepts
are common to other cloud plartforms. Implement this test using the
platform that you feel comfortable with (AWS, Azure, GCE, OpenStack).

Most of the cloud providers have a free tier to test their platform for free, but if you
need some credits feel free to get in touch with us.

** Programming language

Please use a JVM language for your solution (Scala preferred). Take into account that IaaS providers have available SDKs for a few
languages and it could make easier the task if you use them.

** Help

We think this test is clearly defined and we are also interested in seeing how you approach problems.
Feel free to make your own calls and make design decisions.
If you feel stuck or if you need to clarify anything please don't hesitate to reach out.
