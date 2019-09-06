# presto-gateway

A load balancer / proxy / gateway for presto compute engine.


##### Table of Contents

   * [Standalone Gateway](/gateway/#standalone-gateway)
      * [Getting Started](/gateway/#getting-started)
         * [Build and run](/gateway/#build-and-run)
         * [Query History UI - check query plans etc.](/gateway/#query-history-ui---check-query-plans-etc)
         * [Adhoc vs Scheduled query routing](/gateway/#adhoc-vs-scheduled-query-routing)
      * [Gateway API](/gateway/#gateway-api)
         * [Get all backends behind the gateway](/gateway/#get-all-backends-behind-the-gateway)
         * [Get active backends behind the Gateway](/gateway/#get-active-backends-behind-the-gateway)
         * [Deactivate a backend](/gateway/#deactivate-a-backend)
         * [Activate a backend](/gateway/#activate-a-backend)
         
   * [Gateway-HA](/gateway-ha#gateway-ha)
      * [Getting Started](/gateway-ha/#getting-started)
         * [Build and run](/gateway-ha/#build-and-run)
         * [Query History UI - check query plans etc.](/gateway-ha/#query-history-ui---check-query-plans-etc)
         * [Gateway Admin UI - add and modify backend information](/gateway-ha/#gateway-admin-ui---add-and-modify-backend-information)
      * [How to setup a dev environment](/gateway-ha/#how-to-setup-a-dev-environment)
      * [Gateway HA API](/gateway-ha/#gateway-api)
         * [Get all backends behind the gateway](/gateway-ha/#get-all-backends-behind-the-gateway)
         * [Delete a backend from the gateway](/gateway-ha/#delete-a-backend-from-the-gateway)
         * [Add a backend to the gateway](/gateway-ha/#add-a-backend-to-the-gateway)
         * [Update backend information](/gateway-ha/#update-backend-information)
         * [Get active all backend behind the Gateway](/gateway-ha/#get-active-all-backend-behind-the-gateway)
         * [Deactivate a backend](/gateway-ha/#deactivate-a-backend)
         * [Activate a backend](/gateway-ha/#activate-a-backend)   
         
   * [Gateway Design Document](/docs/design.md)
   
   * [Contributing to Presto Gateway](CONTRIBUTING.md)