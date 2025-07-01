<template>
  <div class="content-wrapper">
    <b-loading v-if="loading" :is-full-page="false" :active="loading"/>
    <div v-else class="panel">
      <p class="panel-heading">
        <b-button
          class="is-link"
          icon-pack="fa"
          icon-left="angle-left"
          @click="$router.push('/appengine')"
          :label="$t('go-back')"
        />
      </p>
      <div class="panel-block">
        <section class="media">
          <figure class="media-left">
            <p class="image logo">
              <img :src="task.imageUrl || 'https://bulma.io/assets/images/placeholders/1280x960.png'">
            </p>
          </figure>
          <div class="media-content app-content">
            <div class="content">
              <p>
                <strong class="app-title">{{ task.name }}</strong>
                <br>
                <span v-if="task.authors && task.authors.length > 0">
                    <small v-for="(author, index) in task.authors" :key="index">
                      {{`- ${author.first_name} ${author.last_name}` || $t('app-engine.no-authors')}}
                    </small>
                </span>
              </p>
            </div>
          </div>

          <div class="media-right update-btn">
            <!-- TODO In the future: Implement proper update of Task -->
            <!-- <UploadAppButton btnFunc="Update" /> -->
          </div>
        </section>

        <section class="metadata level">
          <div class="level-item has-text-centered">
            <div>
              <p class="heading">Date</p>
              <p class="title">{{ task.date || $t('unknown') }}</p>
            </div>
          </div>

          <div class="level-item has-text-centered">
            <div>
              <p class="heading">Version</p>
              <p class="title">{{ task.version }}</p>
            </div>
          </div>

          <!-- TODO: Find a way to handle organization (each author belongs to their own Org.) -->
          <!-- <div class="level-item has-text-centered">
              <div>
                  <p class="heading">Organization</p>
                  <p class="title">{{ task.authors[0]['organization'] || $t('unknown') }}</p>
              </div>
          </div> -->
        </section>

        <section>
          <b-collapse
            class="card"
            animation="slide"
          >

            <template #trigger="props">
              <div
                class="card-header"
                role="button"
              >
                <p class="card-header-title">
                  {{ $t("description") }}
                </p>
                <a class="card-header-icon">
                  <b-icon :icon="props.open ? 'menu-down' : 'menu-up'">
                  </b-icon>
                </a>
              </div>
            </template>

            <div class="card-content">
              <div class="content">
                {{ task.description || $t('no-description') }}
              </div>
            </div>
          </b-collapse>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import Task from '@/utils/appengine/task';

export default {
  name: 'AppInfoPage',
  mount() {
    if (this.id) {
      const state = this.getCollapseState();
      if (state) {
        this.isCollapsed = state[this.id];
      }
    }
  },
  data() {
    return {
      task: null,
      loading: true
    };
  },
  async created() {
    // Fetch app data based on the ID from the route
    // const appId = this.$route.params.id;

    // Fetch task data based on Namespace & Version
    const taskNamespace = this.$route.params.namespace;
    const taskVersion = this.$route.params.version;

    this.task = await Task.fetchNamespaceVersion(taskNamespace, taskVersion);
    this.loading = false;
  }
};
</script>

<style scoped>

/* ----- Upper Section (Logo + Update) ----- */
.logo {
  width: 17rem;
  height: 13rem;
  position: relative;
  overflow: hidden;
  /* images that overflow (ex. portrait) will need this one */
  border-radius: 15%;
}

img {
  position: absolute;
  width: 100%;
  top: 50%;
  -ms-transform: translateY(-50%);
  -webkit-transform: translateY(-50%);
  transform: translateY(-50%);
  /* for none overflowing imgs */
  border-radius: 15%;
}

.app-content {
  margin: 1%;
  padding-top: 2%;
}

.app-title {
  font-size: 1.9rem;
}

.update-btn {
  margin: 3%;
  size: 2rem;
}

/*  ----- Metadata Section (Date + Ver...) ----- */
.metadata {
  margin-top: 5%;
}
</style>
