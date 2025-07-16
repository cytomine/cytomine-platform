<template>
  <div class="card">
    <router-link :to="`/appengine/${appData.namespace}/${appData.version}`">
      <div class="card-image img-rounded">
        <figure class="image is-animated is-5by3">
          <img
            :src="appData.imageUrl || 'https://bulma.io/assets/images/placeholders/1280x960.png'"
            alt="Placeholder image"
          >
        </figure>
      </div>
      <div class="card-content">
        <div class="media">
          <div class="media-content">
            <p class="title is-4 less-bottom">{{ appData.name }}</p>
            <time datetime="">{{ appData.date }}</time>
          </div>
          <div class="media-right">
            <p class="subtitle is-6">{{ appData.version }}</p>
          </div>
        </div>

        <div class="content">
          {{ appData.description }}
        </div>

        <footer class="card-footer">
          <a href="#" class="card-footer-item">More</a>
        </footer>
      </div>
    </router-link>
  </div>
</template>

<script>
// to be used in the future: adding pop-out to show info when clicking more...
// import AppCardInfo from './AppCardInfo.vue';


export default {
  name: 'AppCard',
  props: {
    appData: {
      type: Object,
      required: true,
    },
  },
  components: {
    // AppCardInfo
  }
};
</script>

<style scoped>
.less-bottom {
  margin-bottom: 0.9rem !important;
}

.card {
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  transition: .2s ease-out;
}

.rounded {
  border-radius: 10px;
}

/* On mouse-over, add a deeper shadow + Bounce */
.card:hover {
  box-shadow: 0 1px 100px 0 rgba(38, 63, 206, 0.3);
  transform: translate3d(0, -2px, 0);
}

/* Router-Link makes entire text blue so let's select everything except the links & make them black */
:not(a) {
  color: black;
}
</style>
