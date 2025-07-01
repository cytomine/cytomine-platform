<template>
  <div
    class="image-container"
    :class="{ 'selected': isSelected }"
    @click="$emit('update:selected', image.id)"
  >
    <image-thumbnail
      :key="`${image.id}-thumb-128`"
      :image="image"
      :size="128"
      :extra-parameters="{Authorization: 'Bearer ' + shortTermToken}"
    />
    <span>{{ image.instanceFilename }}</span>
  </div>
</template>

<script>
import ImageThumbnail from '@/components/image/ImageThumbnail';
import {get} from '@/utils/store-helpers';

export default {
  name: 'SelectableImage',
  components: {
    ImageThumbnail,
  },
  props: {
    image: {type: Object, required: true},
    isSelected: {type: Boolean, required: true},
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
  }
};
</script>

<style scoped>
.image-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0.5rem;
}

.image-container span {
  margin-top: 2px;
  font-size: 12px;
}

.selected {
  background-color: lightblue;
}
</style>
