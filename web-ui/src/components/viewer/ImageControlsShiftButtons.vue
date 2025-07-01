<template>
  <div class="buttons has-addons" :class="{'backward-buttons': !forward}">
    <button
        class="button is-small item"
        :disabled="!canShift"
        @click="shiftOne()"
    >
      <i class="fas" :class="{'fa-step-forward': forward, 'fa-step-backward': !forward}"></i>
    </button>
    <v-popover
        :placement="selectorPlacement"
        trigger="manual"
        :open="opened"
        :auto-hide="false"
        :popover-inner-class="'tooltip-inner popover-inner step-selector'"
    >
      <button
          class="button is-small item"
          :disabled="!canShift"
          @click="shift()"
          @click.right.prevent="startEdition()"
      >
        <i class="fas" :class="{'fa-fast-forward': forward, 'fa-fast-backward': !forward}"></i>
        <div class="step-counter">
          <template v-if="forward">+</template>
          <template v-else>-</template>{{step}}
        </div>
      </button>

      <template #popover>
        <div
          v-click-outside="() => stopEdition()"
          v-click-outside:contextmenu.capture="() => stopEdition()"
          class="step-selector"
        >
          <b-input :placeholder="$t('step')" size="is-small"
             v-model="editedValue"
             ref="inputStepSelector"
             @hook:mounted="focus()"
             @blur="stopEdition()"
             @keyup.enter.native="stopEdition()"
          />
        </div>
      </template>
    </v-popover>
  </div>
</template>

<script>
export default {
  name: 'ImageControlsShiftButtons',
  props: {
    index: String,
    forward: Boolean,
    current: Number,
    size: Number,
    dimension: String,
  },
  data() {
    return {
      opened: false,
      editedValue: 0,
    };
  },
  computed: {
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    imageWrapper() {
      return this.viewerWrapper.images[this.index];
    },
    step: {
      get() {
        return this.imageWrapper.controls.step[this.dimension];
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setStep',
          {dimension: this.dimension, value: Number(value)});
      }
    },
    canShift() {
      return (this.forward) ? (this.current < this.size - 1) : (this.current >= 1);
    },
    shiftDirection() {
      return (this.forward) ? 1 : -1;
    },
    selectorPlacement() {
      return (this.forward) ? 'left' : 'right';
    }
  },
  methods: {
    shiftOne() {
      this.$emit('shift', this.shiftDirection);
    },
    shift() {
      let value = this.shiftDirection;
      value *= (this.forward) ? Math.min(this.step, this.size - this.current - 1) : Math.min(this.step, this.current);
      this.$emit('shift', value);
    },
    startEdition() {
      this.editedValue = this.step;
      this.opened = true;
    },
    stopEdition() {
      this.opened = false;
      if (!this.editedValue || isNaN(this.editedValue)) {
        return;
      }

      this.step = parseInt(this.editedValue);
    },
    focus() {
      this.$refs.inputStepSelector.focus();
    }
  }
};
</script>

<style lang="scss" scoped>
  .buttons {
    float:left;
    margin: 1px;
    margin-bottom: 0 !important;

    .button {
      margin-bottom: 0;
    }
  }

  .step-counter {
    position: absolute;
    top: 0.25em;
    right: 0.25em;
    font-size: 0.7em;
    font-weight: 600;
    text-align:right;
    line-height: 0.9em;
  }

  .backward-buttons {
    flex-direction: row-reverse;
  }
</style>

<style>
  .step-selector {
    padding: 0.1rem 0.3rem !important;
  }

  .step-selector input {
    width: 4rem;
  }
</style>